package com.mecanica.oficina_billing_service.application.orcamento.gateway;

import com.mecanica.oficina_billing_service.application.orcamento.output.NotificacaoCliente;

public interface NotificadorClienteGateway {
    void notificar(NotificacaoCliente notificacao);
}
