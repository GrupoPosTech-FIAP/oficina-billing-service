package com.mecanica.oficina_billing_service.application.orcamento.input;

import java.util.List;

public record AtualizarOrcamentoInput(
        List<ItemOrcamentoInput> itens,
        List<ItemInsumoInput> insumos,
        List<ItemServicoInput> servicos,
        String observacoes
) {
    public AtualizarOrcamentoInput(List<ItemInsumoInput> insumos, List<ItemServicoInput> servicos, String observacoes) {
        this(null, insumos, servicos, observacoes);
    }
}
