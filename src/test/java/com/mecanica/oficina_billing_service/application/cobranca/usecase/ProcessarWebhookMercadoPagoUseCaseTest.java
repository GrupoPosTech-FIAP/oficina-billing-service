package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.cobranca.input.ProcessarWebhookInput;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessarWebhookMercadoPagoUseCaseTest {

    @Mock
    private CobrancaGateway cobrancaGateway;

    @Mock
    private MercadoPagoGateway mercadoPagoGateway;

    @Mock
    private PublicadorEventosGateway publicador;

    private ProcessarWebhookMercadoPagoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessarWebhookMercadoPagoUseCase(cobrancaGateway, mercadoPagoGateway, publicador);
    }

    @Test
    @DisplayName("Deve atualizar cobrança para PAGO quando status do pagamento for approved")
    void deveAtualizarCobrancaParaPago() {
        var input = new ProcessarWebhookInput("payment.updated", "payment", "987654321");
        String referencia = "oficina-billing:dev:os-999";

        var cobranca = Cobranca.criar("os-999", new BigDecimal("150.00"), "Troca Pastilhas", referencia, "pref-99", "http://init");

        when(mercadoPagoGateway.consultarPagamento(987654321L))
                .thenReturn(Optional.of(new MercadoPagoGateway.DetalhePagamento(987654321L, "approved", "accredited", referencia)));

        when(cobrancaGateway.buscarPorReferencia(referencia)).thenReturn(Optional.of(cobranca));
        when(cobrancaGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.executar(input);

        verify(cobrancaGateway).atualizar(argThat(c -> c.getStatus() == CobrancaStatus.PAGO && c.getPagamentoId() == 987654321L));
        verify(publicador).publicar("PagamentoConfirmado", "os-999", java.util.Map.of(
                "ordemServicoId", "os-999",
                "cobrancaId", cobranca.getId().toString(),
                "mercadoPagoPaymentId", 987654321L,
                "valor", new BigDecimal("150.00")));
    }

    @Test
    @DisplayName("Deve ser idempotente e não atualizar novamente se a cobrança já estiver com status PAGO")
    void deveSerIdempotenteQuandoJaPago() {
        var input = new ProcessarWebhookInput("payment.updated", "payment", "987654321");
        String referencia = "oficina-billing:dev:os-999";

        var cobranca = Cobranca.criar("os-999", new BigDecimal("150.00"), "Troca Pastilhas", referencia, "pref-99", "http://init")
                .marcarComoPago();

        when(mercadoPagoGateway.consultarPagamento(987654321L))
                .thenReturn(Optional.of(new MercadoPagoGateway.DetalhePagamento(987654321L, "approved", "accredited", referencia)));

        when(cobrancaGateway.buscarPorReferencia(referencia)).thenReturn(Optional.of(cobranca));

        useCase.executar(input);

        verify(cobrancaGateway, never()).atualizar(any());
        verifyNoInteractions(publicador);
    }

    @Test
    @DisplayName("Deve atualizar cobrança para RECUSADO quando status do pagamento for rejected")
    void deveAtualizarCobrancaParaRecusado() {
        var input = new ProcessarWebhookInput("payment.updated", "payment", "987654321");
        String referencia = "oficina-billing:dev:os-999";

        var cobranca = Cobranca.criar("os-999", new BigDecimal("150.00"), "Troca Pastilhas", referencia, "pref-99", "http://init");

        when(mercadoPagoGateway.consultarPagamento(987654321L))
                .thenReturn(Optional.of(new MercadoPagoGateway.DetalhePagamento(987654321L, "rejected", "cc_rejected_insufficient_amount", referencia)));

        when(cobrancaGateway.buscarPorReferencia(referencia)).thenReturn(Optional.of(cobranca));
        when(cobrancaGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.executar(input);

        verify(cobrancaGateway).atualizar(argThat(c -> c.getStatus() == CobrancaStatus.RECUSADO));
        verify(publicador).publicar("PagamentoRecusado", "os-999", java.util.Map.of(
                "ordemServicoId", "os-999",
                "cobrancaId", cobranca.getId().toString(),
                "mercadoPagoPaymentId", 987654321L,
                "motivo", "cc_rejected_insufficient_amount"));
    }

    private static final String REFERENCIA = "oficina-billing:dev:os-999";

    private Cobranca pendente() {
        return Cobranca.criar("os-999", new BigDecimal("150.00"), "Troca Pastilhas", REFERENCIA, "pref-99", "http://init");
    }

    private void webhook(long paymentId, String status, Cobranca cobranca) {
        when(mercadoPagoGateway.consultarPagamento(paymentId))
                .thenReturn(Optional.of(new MercadoPagoGateway.DetalhePagamento(paymentId, status, "accredited", REFERENCIA)));
        when(cobrancaGateway.buscarPorReferencia(REFERENCIA)).thenReturn(Optional.of(cobranca));
        useCase.executar(new ProcessarWebhookInput("payment.updated", "payment", String.valueOf(paymentId)));
    }

    @Test
    @DisplayName("Pagamento aprovado depois da expiração é reembolsado e publica PagamentoEstornado")
    void deveReembolsarPagamentoAposExpiracao() {
        var cancelada = pendente().cancelar();
        when(cobrancaGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        webhook(321L, "approved", cancelada);

        verify(mercadoPagoGateway).estornarPagamento(321L);
        verify(cobrancaGateway).atualizar(argThat(c -> c.getStatus() == CobrancaStatus.ESTORNADO && c.getPagamentoId() == 321L));
        verify(publicador).publicar("PagamentoEstornado", "os-999", java.util.Map.of(
                "ordemServicoId", "os-999",
                "cobrancaId", cancelada.getId().toString(),
                "mercadoPagoPaymentId", 321L,
                "valor", new BigDecimal("150.00"),
                "motivo", "PAGAMENTO_APOS_EXPIRACAO"));
        verify(publicador, never()).publicar(eq("PagamentoConfirmado"), any(), any());
    }

    @Test
    @DisplayName("Pagamento aprovado depois da recusa é reembolsado em vez de confirmar a OS cancelada")
    void deveReembolsarPagamentoAposRecusa() {
        when(cobrancaGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        webhook(322L, "approved", pendente().marcarComoRecusado());

        verify(mercadoPagoGateway).estornarPagamento(322L);
        verify(cobrancaGateway).atualizar(argThat(c -> c.getStatus() == CobrancaStatus.ESTORNADO));
        verify(publicador).publicar(eq("PagamentoEstornado"), eq("os-999"),
                argThat(p -> "PAGAMENTO_APOS_RECUSA".equals(p.get("motivo"))));
    }

    @Test
    @DisplayName("Webhook repetido do pagamento tardio já reembolsado não reembolsa de novo")
    void naoReembolsaDuasVezesOPagamentoTardio() {
        webhook(321L, "approved", pendente().cancelar().estornarPagamentoTardio(321L));

        verify(mercadoPagoGateway, never()).estornarPagamento(any());
        verify(cobrancaGateway, never()).atualizar(any());
        verifyNoInteractions(publicador);
    }

    @Test
    @DisplayName("Segundo pagamento de uma cobrança já paga é reembolsado sem mexer na cobrança")
    void deveReembolsarPagamentoDuplicado() {
        webhook(400L, "approved", pendente().marcarComoPago(399L));

        verify(mercadoPagoGateway).estornarPagamento(400L);
        verify(cobrancaGateway, never()).atualizar(any());
        verify(publicador).publicar(eq("PagamentoEstornado"), eq("os-999"),
                argThat(p -> "PAGAMENTO_DUPLICADO".equals(p.get("motivo")) && p.get("mercadoPagoPaymentId").equals(400L)));
    }

    @Test
    @DisplayName("Reembolso de outro pagamento não estorna a cobrança paga")
    void refundDeOutroPagamentoNaoEstornaACobranca() {
        webhook(400L, "refunded", pendente().marcarComoPago(399L));

        verify(cobrancaGateway, never()).atualizar(any());
    }

    @Test
    @DisplayName("Falha no reembolso propaga para o Mercado Pago reenviar o webhook")
    void falhaNoReembolsoPropaga() {
        doThrow(new IllegalStateException("MP fora")).when(mercadoPagoGateway).estornarPagamento(321L);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> webhook(321L, "approved", pendente().cancelar()));

        verify(cobrancaGateway, never()).atualizar(any());
        verifyNoInteractions(publicador);
    }

    @Test
    @DisplayName("Deve atualizar cobrança para ESTORNADO quando status do pagamento for refunded")
    void deveAtualizarCobrancaParaEstornado() {
        var input = new ProcessarWebhookInput("payment.updated", "payment", "987654321");
        String referencia = "oficina-billing:dev:os-999";

        var cobranca = Cobranca.criar("os-999", new BigDecimal("150.00"), "Troca Pastilhas", referencia, "pref-99", "http://init")
                .marcarComoPago();

        when(mercadoPagoGateway.consultarPagamento(987654321L))
                .thenReturn(Optional.of(new MercadoPagoGateway.DetalhePagamento(987654321L, "refunded", "refunded", referencia)));

        when(cobrancaGateway.buscarPorReferencia(referencia)).thenReturn(Optional.of(cobranca));

        useCase.executar(input);

        verify(cobrancaGateway).atualizar(argThat(c -> c.getStatus() == CobrancaStatus.ESTORNADO));
    }
}
