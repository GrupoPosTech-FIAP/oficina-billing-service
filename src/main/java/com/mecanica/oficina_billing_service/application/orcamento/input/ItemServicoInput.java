package com.mecanica.oficina_billing_service.application.orcamento.input;

import java.math.BigDecimal;

public record ItemServicoInput(
        String servicoId,
        int quantidade,
        BigDecimal precoUnitario,
        String descricao
) {
    public ItemServicoInput(String servicoId, int quantidade) {
        this(servicoId, quantidade, null, null);
    }
}
