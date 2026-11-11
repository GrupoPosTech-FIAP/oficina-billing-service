package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.evento.EventosSaga;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.output.NotificacaoCliente;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;

import java.util.Map;

/** Registra a recusa do cliente e publica {@code OrcamentoNegado}: a OS é cancelada. */
public class NegarOrcamentoUseCase {

    private final OrcamentoGateway orcamentoGateway;
    private final NotificadorClienteGateway notificadorClienteGateway;
    private final PublicadorEventosGateway publicador;

    public NegarOrcamentoUseCase(OrcamentoGateway orcamentoGateway, NotificadorClienteGateway notificadorClienteGateway,
                                  PublicadorEventosGateway publicador) {
        this.orcamentoGateway = orcamentoGateway;
        this.notificadorClienteGateway = notificadorClienteGateway;
        this.publicador = publicador;
    }

    public Orcamento executar(String id) {
        Orcamento orcamento = orcamentoGateway.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado com ID: " + id));

        Orcamento negado = orcamento.negar();
        Orcamento salvo = orcamentoGateway.atualizar(negado);

        if (notificadorClienteGateway != null) {
            notificadorClienteGateway.notificar(NotificacaoCliente.orcamentoNegado(salvo));
        }

        publicador.publicar(EventosSaga.ORCAMENTO_NEGADO, salvo.getOrdemServicoId(), Map.of(
                "ordemServicoId", salvo.getOrdemServicoId(),
                "orcamentoId", salvo.getId()));

        return salvo;
    }
}
