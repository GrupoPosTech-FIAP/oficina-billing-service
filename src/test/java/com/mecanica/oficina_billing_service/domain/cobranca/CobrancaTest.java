package com.mecanica.oficina_billing_service.domain.cobranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CobrancaTest {

    @Test
    @DisplayName("Deve criar cobrança com status PENDENTE e dados válidos")
    void deveCriarCobrancaComSucesso() {
        var cobranca = Cobranca.criar(
                "os-123",
                new BigDecimal("250.00"),
                "Revisão de Freios",
                "oficina-billing:dev:os-123",
                "pref-999",
                "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=pref-999"
        );

        assertNotNull(cobranca.getId());
        assertEquals("os-123", cobranca.getPedidoId());
        assertEquals(new BigDecimal("250.00"), cobranca.getValor());
        assertEquals("Revisão de Freios", cobranca.getDescricao());
        assertEquals(CobrancaStatus.PENDENTE, cobranca.getStatus());
        assertEquals("pref-999", cobranca.getPreferenceId());
        assertNotNull(cobranca.getCreatedAt());
    }

    @Test
    @DisplayName("Deve transicionar status para PAGO de forma idempotente")
    void deveMarcarComoPago() {
        var cobranca = Cobranca.criar("os-123", new BigDecimal("100.00"), "Troca Óleo", "ref-1", "pref-1", "http://init");

        var pago = cobranca.marcarComoPago();

        assertEquals(CobrancaStatus.PAGO, pago.getStatus());

        // Idempotência
        var pagoNovamente = pago.marcarComoPago();
        assertEquals(CobrancaStatus.PAGO, pagoNovamente.getStatus());
    }

    @Test
    @DisplayName("Deve transicionar status para RECUSADO")
    void deveMarcarComoRecusado() {
        var cobranca = Cobranca.criar("os-123", new BigDecimal("100.00"), "Troca Óleo", "ref-1", "pref-1", "http://init");

        var recusado = cobranca.marcarComoRecusado();

        assertEquals(CobrancaStatus.RECUSADO, recusado.getStatus());
    }

    @Test
    @DisplayName("Deve estornar apenas se estiver PAGO")
    void deveEstornarCobrancaPaga() {
        var cobranca = Cobranca.criar("os-123", new BigDecimal("100.00"), "Troca Óleo", "ref-1", "pref-1", "http://init")
                .marcarComoPago();

        var estornado = cobranca.estornar();

        assertEquals(CobrancaStatus.ESTORNADO, estornado.getStatus());
    }

    @Test
    @DisplayName("Não deve estornar se estiver PENDENTE")
    void naoDeveEstornarCobrancaPendente() {
        var cobranca = Cobranca.criar("os-123", new BigDecimal("100.00"), "Troca Óleo", "ref-1", "pref-1", "http://init");

        assertThrows(IllegalStateException.class, cobranca::estornar);
    }
}
