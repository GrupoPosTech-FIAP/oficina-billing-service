package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;

import java.util.List;

public class ConsultarOrcamentoUseCase {

    private final OrcamentoGateway orcamentoGateway;

    public ConsultarOrcamentoUseCase(OrcamentoGateway orcamentoGateway) {
        this.orcamentoGateway = orcamentoGateway;
    }

    public Orcamento buscarPorId(String id) {
        return orcamentoGateway.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado com ID: " + id));
    }

    public Orcamento buscarPorOrdemServicoId(String ordemServicoId) {
        return orcamentoGateway.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado para OS: " + ordemServicoId));
    }

    public List<Orcamento> listar() {
        return orcamentoGateway.listar();
    }
}
