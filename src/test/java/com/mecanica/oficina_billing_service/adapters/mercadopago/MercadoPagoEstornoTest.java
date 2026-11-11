package com.mecanica.oficina_billing_service.adapters.mercadopago;

import com.mecanica.oficina_billing_service.adapters.config.MercadoPagoProperties;
import com.mecanica.oficina_billing_service.application.cobranca.exception.MercadoPagoIntegrationException;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MercadoPagoEstornoTest {

    private final MercadoPagoClientGateway gateway = new MercadoPagoClientGateway(propriedades());

    private static MercadoPagoProperties propriedades() {
        MercadoPagoProperties p = new MercadoPagoProperties();
        p.setAccessToken("TEST_MOCK_TOKEN");
        return p;
    }

    @Test
    void estornaComChaveDeIdempotenciaFixaPorPagamento() throws Exception {
        try (MockedConstruction<PaymentRefundClient> clientes = mockConstruction(PaymentRefundClient.class)) {
            gateway.estornarPagamento(123L);

            ArgumentCaptor<MPRequestOptions> opcoes = ArgumentCaptor.forClass(MPRequestOptions.class);
            verify(clientes.constructed().getFirst()).refund(eq(123L), opcoes.capture());
            assertThat(opcoes.getValue().getCustomHeaders()).containsEntry("X-Idempotency-Key", "estorno-123");
        }
    }

    @Test
    void falhaDoSdkViraErroDeIntegracao() {
        try (MockedConstruction<PaymentRefundClient> ignored = mockConstruction(PaymentRefundClient.class,
                (cliente, ctx) -> when(cliente.refund(eq(123L), any(MPRequestOptions.class)))
                        .thenThrow(new MPException("timeout")))) {
            assertThatThrownBy(() -> gateway.estornarPagamento(123L))
                    .isInstanceOf(MercadoPagoIntegrationException.class)
                    .hasMessageContaining("timeout");
        }
    }
}
