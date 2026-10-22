package com.mecanica.oficina_billing_service.adapters.notification;

import com.mecanica.oficina_billing_service.application.orcamento.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_billing_service.application.orcamento.output.NotificacaoCliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificadorClienteLogGateway implements NotificadorClienteGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificadorClienteLogGateway.class);

    @Override
    public void notificar(NotificacaoCliente notificacao) {
        log.info("[NOTIFICACAO CLIENTE] Tipo: {} | Orcamento ID: {} | OS ID: {} | Cliente: {} | Valor: {} | Msg: {}",
                notificacao.tipo(),
                notificacao.orcamentoId(),
                notificacao.ordemServicoId(),
                notificacao.clienteId(),
                notificacao.valorTotal(),
                notificacao.mensagem());
    }
}
