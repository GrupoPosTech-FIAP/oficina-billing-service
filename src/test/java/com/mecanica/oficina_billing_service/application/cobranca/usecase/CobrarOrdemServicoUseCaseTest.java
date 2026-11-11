package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.input.CriarCobrancaInput;
import com.mecanica.oficina_billing_service.application.cobranca.output.CobrancaOutput;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CobrarOrdemServicoUseCaseTest {

    private OrcamentoGateway orcamentoGateway;
    private CobrancaGateway cobrancaGateway;
    private CriarCobrancaUseCase criarCobranca;
    private PublicadorEventosGateway publicador;
    private CobrarOrdemServicoUseCase useCase;

    @BeforeEach
    void setUp() {
        orcamentoGateway = mock(OrcamentoGateway.class);
        cobrancaGateway = mock(CobrancaGateway.class);
        criarCobranca = mock(CriarCobrancaUseCase.class);
        publicador = mock(PublicadorEventosGateway.class);
        useCase = new CobrarOrdemServicoUseCase(orcamentoGateway, cobrancaGateway, criarCobranca, publicador);
    }

    private void orcamento(OrcamentoStatus status) {
        when(orcamentoGateway.buscarPorOrdemServicoId("os-1")).thenReturn(Optional.of(Orcamento.reconstituir(
                "orc-1", "os-1", "cli-1", List.of(new ItemOrcamento(null, "srv-1", "Revisão", 1, new BigDecimal("250.00"))),
                status, null, LocalDateTime.now(), null)));
    }

    @Test
    void criaCobrancaDoOrcamentoAprovadoEPublicaCobrancaCriada() {
        when(cobrancaGateway.buscarPorPedidoId("os-1")).thenReturn(Optional.empty());
        orcamento(OrcamentoStatus.APROVADO);
        Cobranca cobranca = Cobranca.criar("os-1", new BigDecimal("250.00"), "Ordem de serviço os-1",
                "oficina-billing:dev:os-1", "pref-1", "https://mp/checkout");
        when(criarCobranca.executar(new CriarCobrancaInput("os-1", "Ordem de serviço os-1", new BigDecimal("250.00"))))
                .thenReturn(CobrancaOutput.fromDomain(cobranca));

        assertThat(useCase.executar("os-1")).isPresent();

        verify(publicador).publicar("CobrancaCriada", "os-1", Map.of(
                "ordemServicoId", "os-1",
                "cobrancaId", cobranca.getId().toString(),
                "valor", new BigDecimal("250.00"),
                "checkoutUrl", "https://mp/checkout"));
    }

    @Test
    void cobrancaJaExistenteNaoCriaOutra() {
        when(cobrancaGateway.buscarPorPedidoId("os-1")).thenReturn(Optional.of(Cobranca.criar("os-1", BigDecimal.TEN,
                null, "ref", "pref", "url")));

        assertThat(useCase.executar("os-1")).isEmpty();

        verifyNoInteractions(criarCobranca, publicador);
    }

    @Test
    void exigeOrcamentoAprovado() {
        when(cobrancaGateway.buscarPorPedidoId("os-1")).thenReturn(Optional.empty());
        orcamento(OrcamentoStatus.NEGADO);

        assertThatThrownBy(() -> useCase.executar("os-1")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(criarCobranca, publicador);
    }

    @Test
    void exigeOrcamento() {
        when(cobrancaGateway.buscarPorPedidoId("os-1")).thenReturn(Optional.empty());
        when(orcamentoGateway.buscarPorOrdemServicoId("os-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar("os-1")).isInstanceOf(IllegalStateException.class);
    }
}
