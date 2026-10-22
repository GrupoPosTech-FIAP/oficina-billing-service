package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.orcamento.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.output.NotificacaoCliente;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;

public class NegarOrcamentoUseCase {

    private final OrcamentoGateway orcamentoGateway;
    private final NotificadorClienteGateway notificadorClienteGateway;

    public NegarOrcamentoUseCase(OrcamentoGateway orcamentoGateway, NotificadorClienteGateway notificadorClienteGateway) {
        this.orcamentoGateway = orcamentoGateway;
        this.notificadorClienteGateway = notificadorClienteGateway;
    }

    public Orcamento executar(String id) {
        Orcamento orcamento = orcamentoGateway.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado com ID: " + id));

        Orcamento negado = orcamento.negar();
        Orcamento salvo = orcamentoGateway.atualizar(negado);

        if (notificadorClienteGateway != null) {
            notificadorClienteGateway.notificar(NotificacaoCliente.orcamentoNegado(salvo));
        }

        return salvo;
    }
}
