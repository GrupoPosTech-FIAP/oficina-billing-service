package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.domain.orcamento.ItemOrcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Eventos da Saga publicados quando o orçamento é enviado, aprovado ou negado. */
class RespostaOrcamentoEventosTest {

    private OrcamentoGateway orcamentoGateway;
    private PublicadorEventosGateway publicador;
    private NotificadorClienteGateway notificador;

    @BeforeEach
    void setUp() {
        orcamentoGateway = mock(OrcamentoGateway.class);
        publicador = mock(PublicadorEventosGateway.class);
        notificador = mock(NotificadorClienteGateway.class);
        when(orcamentoGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Orcamento orcamento(OrcamentoStatus status) {
        Orcamento o = Orcamento.reconstituir("orc-1", "os-1", "cli-1", List.of(
                new ItemOrcamento("it-1", "ins-1", null, "Óleo", 4, new BigDecimal("45.00")),
                new ItemOrcamento("it-2", null, "srv-1", "Troca de óleo", 1, new BigDecimal("80.00"))),
                status, null, LocalDateTime.now(), null);
        when(orcamentoGateway.buscarPorId("orc-1")).thenReturn(Optional.of(o));
        return o;
    }

    @Test
    void envioPublicaOrcamentoGeradoComValorTotal() {
        orcamento(OrcamentoStatus.PENDENTE);

        Orcamento enviado = new EnviarOrcamentoUseCase(orcamentoGateway, notificador, publicador).executar("orc-1");

        assertThat(enviado.getStatus()).isEqualTo(OrcamentoStatus.ENVIADO);
        verify(publicador).publicar("OrcamentoGerado", "os-1", Map.of(
                "ordemServicoId", "os-1", "orcamentoId", "orc-1", "valorTotal", new BigDecimal("260.00")));
    }

    @Test
    void aprovacaoPublicaOrcamentoAprovadoSoComOsInsumos() {
        orcamento(OrcamentoStatus.ENVIADO);

        Orcamento aprovado = new AprovarOrcamentoUseCase(orcamentoGateway, notificador, publicador).executar("orc-1");

        assertThat(aprovado.getStatus()).isEqualTo(OrcamentoStatus.APROVADO);
        verify(publicador).publicar("OrcamentoAprovado", "os-1", Map.of(
                "ordemServicoId", "os-1",
                "orcamentoId", "orc-1",
                "insumos", List.of(Map.of("insumoId", "ins-1", "quantidade", 4))));
    }

    @Test
    void negacaoPublicaOrcamentoNegado() {
        orcamento(OrcamentoStatus.AGUARDANDO);

        Orcamento negado = new NegarOrcamentoUseCase(orcamentoGateway, notificador, publicador).executar("orc-1");

        assertThat(negado.getStatus()).isEqualTo(OrcamentoStatus.NEGADO);
        verify(publicador).publicar("OrcamentoNegado", "os-1", Map.of("ordemServicoId", "os-1", "orcamentoId", "orc-1"));
    }
}
