package com.mecanica.oficina_billing_service.application.orcamento.output;

import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;

import java.math.BigDecimal;

public record NotificacaoCliente(
        TipoNotificacaoCliente tipo,
        String orcamentoId,
        String ordemServicoId,
        String clienteId,
        BigDecimal valorTotal,
        String mensagem) {

    public static NotificacaoCliente envioOrcamento(Orcamento orcamento) {
        return new NotificacaoCliente(
                TipoNotificacaoCliente.ORCAMENTO_ENVIADO,
                orcamento.getId(),
                orcamento.getOrdemServicoId(),
                orcamento.getClienteId(),
                orcamento.getValorTotal(),
                "Orçamento gerado com sucesso no valor de R$ " + orcamento.getValorTotal() + ". Aguardando sua aprovação."
        );
    }

    public static NotificacaoCliente orcamentoAprovado(Orcamento orcamento) {
        return new NotificacaoCliente(
                TipoNotificacaoCliente.ORCAMENTO_APROVADO,
                orcamento.getId(),
                orcamento.getOrdemServicoId(),
                orcamento.getClienteId(),
                orcamento.getValorTotal(),
                "Orçamento aprovado com sucesso."
        );
    }

    public static NotificacaoCliente orcamentoNegado(Orcamento orcamento) {
        return new NotificacaoCliente(
                TipoNotificacaoCliente.ORCAMENTO_NEGADO,
                orcamento.getId(),
                orcamento.getOrdemServicoId(),
                orcamento.getClienteId(),
                orcamento.getValorTotal(),
                "Orçamento rejeitado pelo cliente."
        );
    }
}
