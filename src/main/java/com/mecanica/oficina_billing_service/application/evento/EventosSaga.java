package com.mecanica.oficina_billing_service.application.evento;

/** Nomes dos eventos da Saga (catálogo da ADR 005 em oficina-app-api) usados por este serviço. */
public final class EventosSaga {

    // Consumidos
    public static final String DIAGNOSTICO_CONCLUIDO = "DiagnosticoConcluido";
    public static final String ESTOQUE_BAIXADO = "EstoqueBaixado";
    public static final String EXECUCAO_FALHOU = "ExecucaoFalhou";

    // Publicados
    public static final String ORCAMENTO_GERADO = "OrcamentoGerado";
    public static final String ORCAMENTO_APROVADO = "OrcamentoAprovado";
    public static final String ORCAMENTO_NEGADO = "OrcamentoNegado";
    public static final String ORCAMENTO_EXPIRADO = "OrcamentoExpirado";
    public static final String COBRANCA_CRIADA = "CobrancaCriada";
    public static final String PAGAMENTO_CONFIRMADO = "PagamentoConfirmado";
    public static final String PAGAMENTO_RECUSADO = "PagamentoRecusado";
    public static final String PAGAMENTO_EXPIRADO = "PagamentoExpirado";
    public static final String PAGAMENTO_ESTORNADO = "PagamentoEstornado";

    private EventosSaga() {
    }
}
