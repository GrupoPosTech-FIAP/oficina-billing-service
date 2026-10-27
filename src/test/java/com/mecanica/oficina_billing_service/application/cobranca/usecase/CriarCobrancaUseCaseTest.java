package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.cobranca.input.CriarCobrancaInput;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CriarCobrancaUseCaseTest {

    @Mock
    private CobrancaGateway cobrancaGateway;

    @Mock
    private MercadoPagoGateway mercadoPagoGateway;

    private CriarCobrancaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CriarCobrancaUseCase(cobrancaGateway, mercadoPagoGateway);
    }

    @Test
    @DisplayName("Deve criar cobrança chamando gateway do Mercado Pago e persistindo dados")
    void deveCriarCobrancaComSucesso() {
        var input = new CriarCobrancaInput("os-456", "Revisão geral", new BigDecimal("300.00"),
                "cliente@email.com", "Carlos Silva", "12345678900");
        String referenciaEsperada = "oficina-billing:dev:os-456";

        when(mercadoPagoGateway.isSandboxMode()).thenReturn(true);
        when(mercadoPagoGateway.criarPreferencia(
                eq(referenciaEsperada),
                eq("Revisão geral"),
                eq(new BigDecimal("300.00")),
                eq("cliente@email.com"),
                eq("Carlos Silva"),
                eq("12345678900"),
                isNull()
        )).thenReturn(new MercadoPagoGateway.PreferenciaPagamento(
                "pref-123",
                "https://mp.com/checkout/123",
                "https://sandbox.mp.com/checkout/123"
        ));

        when(cobrancaGateway.salvar(any(Cobranca.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var output = useCase.executar(input);

        assertNotNull(output);
        assertNotNull(output.id());
        assertEquals("os-456", output.pedidoId());
        assertEquals(new BigDecimal("300.00"), output.valor());
        assertEquals(referenciaEsperada, output.referencia());
        assertEquals("pref-123", output.preferenceId());
        // Em modo sandbox, link de pagamento deve ser sandboxInitPoint
        assertEquals("https://sandbox.mp.com/checkout/123", output.initPoint());
        assertEquals(CobrancaStatus.PENDENTE, output.status());

        verify(cobrancaGateway).salvar(any(Cobranca.class));
    }
}
