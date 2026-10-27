package com.mecanica.oficina_billing_service.application.cobranca.input;

import java.math.BigDecimal;

/**
 * Objeto de entrada para o caso de uso de criação de cobrança com Checkout Pro.
 */
public record CriarCobrancaInput(
        String pedidoId,
        String descricao,
        BigDecimal valor,
        String payerEmail,
        String payerName,
        String payerCpf,
        String returnUrl
) {
    /**
     * Construtor de conveniência para chamadas simplificadas (compatibilidade retroativa).
     */
    public CriarCobrancaInput(String pedidoId, String descricao, BigDecimal valor) {
        this(pedidoId, descricao, valor, null, null, null, null);
    }

    public CriarCobrancaInput(String pedidoId, String descricao, BigDecimal valor,
                              String payerEmail, String payerName, String payerCpf) {
        this(pedidoId, descricao, valor, payerEmail, payerName, payerCpf, null);
    }
}
