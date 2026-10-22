package com.mecanica.oficina_billing_service.application.orcamento.input;

import java.util.List;

public record GerarOrcamentoInput(
        String ordemServicoId,
        String clienteId,
        List<ItemOrcamentoInput> itens,
        List<ItemInsumoInput> insumos,
        List<ItemServicoInput> servicos,
        String observacoes
) {
    public GerarOrcamentoInput(String ordemServicoId, String clienteId, List<ItemInsumoInput> insumos, List<ItemServicoInput> servicos, String observacoes) {
        this(ordemServicoId, clienteId, null, insumos, servicos, observacoes);
    }
}
