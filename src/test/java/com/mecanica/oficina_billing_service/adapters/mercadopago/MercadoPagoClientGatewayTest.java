package com.mecanica.oficina_billing_service.adapters.mercadopago;

import com.mecanica.oficina_billing_service.adapters.config.MercadoPagoProperties;
import com.mecanica.oficina_billing_service.application.cobranca.exception.MercadoPagoIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MercadoPagoClientGatewayTest {

    private MercadoPagoProperties properties;
    private MercadoPagoClientGateway gateway;

    @BeforeEach
    void setUp() {
        properties = new MercadoPagoProperties();
        properties.setAccessToken("TEST_MOCK_TOKEN");
        properties.setPublicKey("TEST_PUBLIC_KEY");
        properties.setWebhookPublicUrl("https://oficina.example.com/webhooks/mercadopago");
        properties.setBackUrlSuccess("https://oficina.example.com/checkout/sucesso.html");
        properties.setBackUrlFailure("https://oficina.example.com/checkout/falha.html");
        properties.setBackUrlPending("https://oficina.example.com/checkout/pendente.html");
        properties.setSandboxMode(true);

        gateway = new MercadoPagoClientGateway(properties);
    }

    @Test
    @DisplayName("Deve expor a chave pública e o status de sandbox configurados")
    void deveExporChavePublicaESandbox() {
        assertEquals("TEST_PUBLIC_KEY", gateway.getPublicKey());
        assertTrue(gateway.isSandboxMode());
    }

    @Test
    @DisplayName("Deve lançar MercadoPagoIntegrationException quando a chamada de criação falhar com token de teste inválido")
    void deveLancarExcecaoQuandoCriacaoFalhar() {
        // Chamada real sem rede/token válido deve ser interceptada e encapsulada em MercadoPagoIntegrationException
        assertThrows(MercadoPagoIntegrationException.class, () ->
                gateway.criarPreferencia(
                        "ref-test-001",
                        "Troca de óleo",
                        new BigDecimal("150.00"),
                        "cliente@email.com",
                        "João da Silva",
                        "12345678909",
                        "https://minha-app.com/retorno"
                )
        );
    }

    @Test
    @DisplayName("Deve retornar Optional.empty() quando consulta de pagamento falhar ou ID for inexistente")
    void deveRetornarEmptyQuandoPagamentoInexistente() {
        var detalhe = gateway.consultarPagamento(9999999999L);
        assertTrue(detalhe.isEmpty());
    }

    @Test
    @DisplayName("Deve lançar MercadoPagoIntegrationException quando MP_ACCESS_TOKEN não estiver configurado")
    void deveLancarExcecaoQuandoTokenNaoConfigurado() {
        var emptyProps = new MercadoPagoProperties();
        emptyProps.setAccessToken("");
        var emptyGateway = new MercadoPagoClientGateway(emptyProps);
        com.mercadopago.MercadoPagoConfig.setAccessToken("");

        var ex = assertThrows(MercadoPagoIntegrationException.class, () ->
                emptyGateway.criarPreferencia(
                        "ref-001",
                        "Teste",
                        new BigDecimal("100.00"),
                        null,
                        null,
                        null,
                        null
                )
        );
        assertTrue(ex.getMessage().contains("MP_ACCESS_TOKEN"));
    }
}
