package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.evento.EventosSaga;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.output.NotificacaoCliente;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;

import java.util.Map;

/** Envia o orçamento ao cliente e publica {@code OrcamentoGerado}: a OS passa a aguardar aprovação. */
public class EnviarOrcamentoUseCase {

    private final OrcamentoGateway orcamentoGateway;
    private final NotificadorClienteGateway notificadorClienteGateway;
    private final PublicadorEventosGateway publicador;

    public EnviarOrcamentoUseCase(OrcamentoGateway orcamentoGateway, NotificadorClienteGateway notificadorClienteGateway,
                                  PublicadorEventosGateway publicador) {
        this.orcamentoGateway = orcamentoGateway;
        this.notificadorClienteGateway = notificadorClienteGateway;
        this.publicador = publicador;
    }

    public Orcamento executar(String id) {
        Orcamento orcamento = orcamentoGateway.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado com ID: " + id));

        Orcamento enviado = orcamento.enviar();
        Orcamento salvo = orcamentoGateway.atualizar(enviado);

        if (notificadorClienteGateway != null) {
            notificadorClienteGateway.notificar(NotificacaoCliente.envioOrcamento(salvo));
        }

        publicador.publicar(EventosSaga.ORCAMENTO_GERADO, salvo.getOrdemServicoId(), Map.of(
                "ordemServicoId", salvo.getOrdemServicoId(),
                "orcamentoId", salvo.getId(),
                "valorTotal", salvo.getValorTotal()));

        return salvo;
    }
}
