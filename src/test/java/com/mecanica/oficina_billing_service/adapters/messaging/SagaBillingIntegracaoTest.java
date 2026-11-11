package com.mecanica.oficina_billing_service.adapters.messaging;

import com.mecanica.oficina_billing_service.adapters.persistence.OutboxEventoJpaEntity;
import com.mecanica.oficina_billing_service.adapters.persistence.OutboxEventoSpringDataRepository;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.cobranca.input.ProcessarWebhookInput;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ExpirarCobrancasUseCase;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ProcessarWebhookMercadoPagoUseCase;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.usecase.AprovarOrcamentoUseCase;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Passos da Saga no billing de ponta a ponta (sem broker, Mercado Pago simulado):
 * diagnóstico → orçamento → aprovação → cobrança → pagamento → estorno.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SagaBillingIntegracaoTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private ProcessadorEventos processador;
    @Autowired
    private OutboxEventoSpringDataRepository outbox;
    @Autowired
    private OrcamentoGateway orcamentos;
    @Autowired
    private CobrancaGateway cobrancas;
    @Autowired
    private AprovarOrcamentoUseCase aprovarOrcamento;
    @Autowired
    private ProcessarWebhookMercadoPagoUseCase processarWebhook;
    @Autowired
    private TransactionTemplate transacao;
    @Autowired
    private PublicadorEventosGateway publicador;

    @MockitoBean
    private MercadoPagoGateway mercadoPago;

    private void receber(String eventType, String sagaId, String payload) {
        processador.processar(new EventoEnvelope(UUID.randomUUID().toString(), eventType, 1, Instant.now(),
                "oficina-app-api", sagaId, JSON.readTree(payload)));
    }

    private List<String> eventosPublicados(String sagaId) {
        return outbox.findAll().stream()
                .filter(e -> e.getSagaId().equals(sagaId))
                .sorted(Comparator.comparing(OutboxEventoJpaEntity::getCriadoEm))
                .map(OutboxEventoJpaEntity::getEventType)
                .toList();
    }

    @Test
    void fluxoCompletoComEstornoQuandoAExecucaoFalha() {
        String os = UUID.randomUUID().toString();

        receber("DiagnosticoConcluido", os, """
                {"ordemServicoId":"%s","clienteId":"cli-1","veiculoId":"vei-1",
                 "servicos":[{"servicoId":"srv-1","quantidade":1,"descricao":"Troca de óleo","precoUnitario":80.00}],
                 "insumos":[{"insumoId":"ins-1","quantidade":4,"descricao":"Óleo 5W30","precoUnitario":45.00}]}
                """.formatted(os));

        Orcamento orcamento = orcamentos.buscarPorOrdemServicoId(os).orElseThrow();
        assertThat(orcamento.getStatus()).isEqualTo(OrcamentoStatus.ENVIADO);
        assertThat(orcamento.getValorTotal()).isEqualByComparingTo("260.00");
        assertThat(eventosPublicados(os)).containsExactly("OrcamentoGerado");

        transacao.executeWithoutResult(s -> aprovarOrcamento.executar(orcamento.getId()));
        assertThat(eventosPublicados(os)).endsWith("OrcamentoAprovado");

        when(mercadoPago.criarPreferencia(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new MercadoPagoGateway.PreferenciaPagamento("pref-" + os, "https://mp/checkout/" + os));
        receber("EstoqueBaixado", os, "{\"ordemServicoId\":\"" + os + "\",\"itens\":[]}");

        var cobranca = cobrancas.buscarPorPedidoId(os).orElseThrow();
        assertThat(cobranca.getValor()).isEqualByComparingTo("260.00");
        assertThat(eventosPublicados(os)).endsWith("CobrancaCriada");

        when(mercadoPago.consultarPagamento(555L)).thenReturn(Optional.of(new MercadoPagoGateway.DetalhePagamento(
                555L, "approved", "accredited", cobranca.getReferencia())));
        transacao.executeWithoutResult(s -> processarWebhook.executar(new ProcessarWebhookInput("payment.updated", "payment", "555")));
        assertThat(eventosPublicados(os)).endsWith("PagamentoConfirmado");

        receber("ExecucaoFalhou", os, "{\"ordemServicoId\":\"" + os + "\",\"motivo\":\"peça incompatível\"}");

        verify(mercadoPago).estornarPagamento(555L);
        assertThat(cobrancas.buscarPorPedidoId(os).orElseThrow().getStatus()).isEqualTo(CobrancaStatus.ESTORNADO);
        assertThat(eventosPublicados(os)).containsExactly(
                "OrcamentoGerado", "OrcamentoAprovado", "CobrancaCriada", "PagamentoConfirmado", "PagamentoEstornado");
    }

    @Test
    void pagamentoAprovadoDepoisDaExpiracaoEReembolsado() {
        String os = UUID.randomUUID().toString();
        receber("DiagnosticoConcluido", os, """
                {"ordemServicoId":"%s","clienteId":"cli-1",
                 "servicos":[{"servicoId":"srv-1","quantidade":1,"descricao":"Revisão","precoUnitario":100}],"insumos":[]}
                """.formatted(os));
        Orcamento orcamento = orcamentos.buscarPorOrdemServicoId(os).orElseThrow();
        transacao.executeWithoutResult(s -> aprovarOrcamento.executar(orcamento.getId()));
        when(mercadoPago.criarPreferencia(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new MercadoPagoGateway.PreferenciaPagamento("pref-" + os, "https://mp/checkout/" + os));
        receber("EstoqueBaixado", os, "{\"ordemServicoId\":\"" + os + "\",\"itens\":[]}");

        // prazo zero: o agendador cancela a cobrança recém-criada como se o prazo tivesse passado
        var expirar = new ExpirarCobrancasUseCase(cobrancas, publicador, Duration.ZERO, Clock.systemUTC());
        transacao.executeWithoutResult(s -> expirar.executar());
        assertThat(cobrancas.buscarPorPedidoId(os).orElseThrow().getStatus()).isEqualTo(CobrancaStatus.CANCELADO);

        var cobranca = cobrancas.buscarPorPedidoId(os).orElseThrow();
        when(mercadoPago.consultarPagamento(777L)).thenReturn(Optional.of(new MercadoPagoGateway.DetalhePagamento(
                777L, "approved", "accredited", cobranca.getReferencia())));
        transacao.executeWithoutResult(s -> processarWebhook.executar(new ProcessarWebhookInput("payment.updated", "payment", "777")));
        transacao.executeWithoutResult(s -> processarWebhook.executar(new ProcessarWebhookInput("payment.updated", "payment", "777")));

        verify(mercadoPago, times(1)).estornarPagamento(777L);
        var estornada = cobrancas.buscarPorPedidoId(os).orElseThrow();
        assertThat(estornada.getStatus()).isEqualTo(CobrancaStatus.ESTORNADO);
        assertThat(estornada.getPagamentoId()).isEqualTo(777L);
        assertThat(eventosPublicados(os)).containsExactly(
                "OrcamentoGerado", "OrcamentoAprovado", "CobrancaCriada", "PagamentoExpirado", "PagamentoEstornado");
    }

    @Test
    void diagnosticoReentregueNaoGeraSegundoOrcamento() {
        String os = UUID.randomUUID().toString();
        String payload = """
                {"ordemServicoId":"%s","clienteId":"cli-1",
                 "servicos":[{"servicoId":"srv-1","quantidade":1,"descricao":"Revisão","precoUnitario":100}],"insumos":[]}
                """.formatted(os);

        receber("DiagnosticoConcluido", os, payload);
        receber("DiagnosticoConcluido", os, payload);

        assertThat(eventosPublicados(os)).containsExactly("OrcamentoGerado");
    }
}
