package com.mecanica.oficina_billing_service.application.cobranca.usecase;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessarWebhookMercadoPagoUseCaseTest {

    @Mock
    private CobrancaGateway cobrancaGateway;

    @Mock
    private MercadoPagoGateway mercadoPagoGateway;

    private ProcessarWebhookMercadoPagoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessarWebhookMercadoPagoUseCase(cobrancaGateway, mercadoPagoGateway);
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

        useCase.executar(input);

        verify(cobrancaGateway).atualizar(argThat(c -> c.getStatus() == CobrancaStatus.PAGO));
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

        useCase.executar(input);

        verify(cobrancaGateway).atualizar(argThat(c -> c.getStatus() == CobrancaStatus.RECUSADO));
    }
}
