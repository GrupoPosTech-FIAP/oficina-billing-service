package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.evento.EventosSaga;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.output.NotificacaoCliente;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;

import java.util.Map;

/** Aprova o orçamento e publica {@code OrcamentoAprovado} com os insumos a baixar no estoque. */
public class AprovarOrcamentoUseCase {

    private final OrcamentoGateway orcamentoGateway;
    private final NotificadorClienteGateway notificadorClienteGateway;
    private final PublicadorEventosGateway publicador;

    public AprovarOrcamentoUseCase(OrcamentoGateway orcamentoGateway, NotificadorClienteGateway notificadorClienteGateway,
                                  PublicadorEventosGateway publicador) {
        this.orcamentoGateway = orcamentoGateway;
        this.notificadorClienteGateway = notificadorClienteGateway;
        this.publicador = publicador;
    }

    public Orcamento executar(String id) {
        Orcamento orcamento = orcamentoGateway.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado com ID: " + id));

        Orcamento aprovado = orcamento.aprovar();
        Orcamento salvo = orcamentoGateway.atualizar(aprovado);

        if (notificadorClienteGateway != null) {
            notificadorClienteGateway.notificar(NotificacaoCliente.orcamentoAprovado(salvo));
        }

        publicador.publicar(EventosSaga.ORCAMENTO_APROVADO, salvo.getOrdemServicoId(), Map.of(
                "ordemServicoId", salvo.getOrdemServicoId(),
                "orcamentoId", salvo.getId(),
                "insumos", salvo.getItens().stream()
                        .filter(item -> item.getInsumoId() != null)
                        .<Map<String, Object>>map(item -> Map.of("insumoId", item.getInsumoId(), "quantidade", item.getQuantidade()))
                        .toList()));

        return salvo;
    }
}
