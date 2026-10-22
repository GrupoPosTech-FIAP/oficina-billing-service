package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;

public class AguardarOrcamentoUseCase {

    private final OrcamentoGateway orcamentoGateway;

    public AguardarOrcamentoUseCase(OrcamentoGateway orcamentoGateway) {
        this.orcamentoGateway = orcamentoGateway;
    }

    public Orcamento executar(String id) {
        Orcamento orcamento = orcamentoGateway.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado com ID: " + id));

        Orcamento emAguardo = orcamento.aguardar();
        return orcamentoGateway.atualizar(emAguardo);
    }
}
