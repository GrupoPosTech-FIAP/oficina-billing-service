package com.mecanica.oficina_billing_service.application.orcamento.input;

import java.math.BigDecimal;

public record ItemInsumoInput(
        String insumoId,
        int quantidade,
        BigDecimal precoUnitario,
        String descricao
) {
    public ItemInsumoInput(String insumoId, int quantidade) {
        this(insumoId, quantidade, null, null);
    }
}
