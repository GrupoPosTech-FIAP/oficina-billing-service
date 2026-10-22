package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.orcamento.MontadorItensOrcamento;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.input.AtualizarOrcamentoInput;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;

public class AtualizarOrcamentoUseCase {

    private final OrcamentoGateway orcamentoGateway;
    private final MontadorItensOrcamento montadorItensOrcamento;

    public AtualizarOrcamentoUseCase(OrcamentoGateway orcamentoGateway, MontadorItensOrcamento montadorItensOrcamento) {
        this.orcamentoGateway = orcamentoGateway;
        this.montadorItensOrcamento = montadorItensOrcamento;
    }

    public Orcamento executar(String id, AtualizarOrcamentoInput input) {
        Orcamento orcamento = orcamentoGateway.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado com ID: " + id));

        var novosItens = montadorItensOrcamento.montar(input);
        Orcamento atualizado = orcamento.atualizar(novosItens, input.observacoes());

        return orcamentoGateway.atualizar(atualizado);
    }
}
