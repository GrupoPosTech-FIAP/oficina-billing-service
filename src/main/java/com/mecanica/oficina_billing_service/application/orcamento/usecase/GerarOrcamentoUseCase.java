package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.orcamento.MontadorItensOrcamento;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.input.GerarOrcamentoInput;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;

public class GerarOrcamentoUseCase {

    private final OrcamentoGateway orcamentoGateway;
    private final MontadorItensOrcamento montadorItensOrcamento;

    public GerarOrcamentoUseCase(OrcamentoGateway orcamentoGateway, MontadorItensOrcamento montadorItensOrcamento) {
        this.orcamentoGateway = orcamentoGateway;
        this.montadorItensOrcamento = montadorItensOrcamento;
    }

    public Orcamento executar(GerarOrcamentoInput input) {
        var existenteOpt = orcamentoGateway.buscarPorOrdemServicoId(input.ordemServicoId());
        if (existenteOpt.isPresent()) {
            var existente = existenteOpt.get();
            if (existente.getStatus() != OrcamentoStatus.NEGADO) {
                throw new IllegalStateException("Ordem de serviço já possui orçamento em andamento (ID: " + existente.getId() + "). Utilize atualização caso necessário.");
            }
        }

        var itens = montadorItensOrcamento.montar(input);
        Orcamento orcamento = Orcamento.criar(input.ordemServicoId(), input.clienteId(), itens, input.observacoes());
        return orcamentoGateway.salvar(orcamento);
    }
}
