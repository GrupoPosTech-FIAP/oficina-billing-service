package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EstornarPagamentoOrdemServicoUseCaseTest {

    private CobrancaGateway cobrancaGateway;
    private MercadoPagoGateway mercadoPago;
    private PublicadorEventosGateway publicador;
    private EstornarPagamentoOrdemServicoUseCase useCase;

    @BeforeEach
    void setUp() {
        cobrancaGateway = mock(CobrancaGateway.class);
        mercadoPago = mock(MercadoPagoGateway.class);
        publicador = mock(PublicadorEventosGateway.class);
        useCase = new EstornarPagamentoOrdemServicoUseCase(cobrancaGateway, mercadoPago, publicador);
        when(cobrancaGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Cobranca pendente() {
        return Cobranca.criar("os-1", new BigDecimal("250.00"), null, "ref", "pref", "url");
    }

    @Test
    void reembolsaNoMercadoPagoMarcaEstornadoEPublicaPagamentoEstornado() {
        Cobranca paga = pendente().marcarComoPago(777L);
        when(cobrancaGateway.buscarPorPedidoId("os-1")).thenReturn(Optional.of(paga));

        assertThat(useCase.executar("os-1")).isTrue();

        verify(mercadoPago).estornarPagamento(777L);
        verify(cobrancaGateway).atualizar(argThat(c -> c.getStatus() == CobrancaStatus.ESTORNADO && c.getPagamentoId() == 777L));
        verify(publicador).publicar("PagamentoEstornado", "os-1", Map.of(
                "ordemServicoId", "os-1",
                "cobrancaId", paga.getId().toString(),
                "mercadoPagoPaymentId", 777L,
                "valor", new BigDecimal("250.00")));
    }

    @Test
    void semCobrancaOuNaoPagaNaoHaEstorno() {
        when(cobrancaGateway.buscarPorPedidoId("os-1")).thenReturn(Optional.empty());
        assertThat(useCase.executar("os-1")).isFalse();

        when(cobrancaGateway.buscarPorPedidoId("os-1")).thenReturn(Optional.of(pendente()));
        assertThat(useCase.executar("os-1")).isFalse();

        verifyNoInteractions(mercadoPago, publicador);
    }

    @Test
    void pagaSemIdDePagamentoFalhaParaIntervencaoManual() {
        when(cobrancaGateway.buscarPorPedidoId("os-1")).thenReturn(Optional.of(pendente().marcarComoPago()));

        assertThatThrownBy(() -> useCase.executar("os-1")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(mercadoPago, publicador);
    }
}
