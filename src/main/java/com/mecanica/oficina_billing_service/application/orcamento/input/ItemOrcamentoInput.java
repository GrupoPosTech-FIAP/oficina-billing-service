package com.mecanica.oficina_billing_service.application.orcamento.input;

import java.math.BigDecimal;

public record ItemOrcamentoInput(
        String descricao,
        int quantidade,
        BigDecimal precoUnitario,
        String insumoId,
        String servicoId
) {
    public ItemOrcamentoInput(String descricao, int quantidade, BigDecimal precoUnitario) {
        this(descricao, quantidade, precoUnitario, null, null);
    }
}
