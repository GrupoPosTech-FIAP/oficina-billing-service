package com.mecanica.oficina_billing_service.adapters.messaging;

import com.mecanica.oficina_billing_service.application.cobranca.usecase.ExpirarCobrancasUseCase;
import com.mecanica.oficina_billing_service.application.orcamento.usecase.ExpirarOrcamentosUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Gera os timeouts da Saga ({@code OrcamentoExpirado}, {@code PagamentoExpirado}). Registrado só
 * com a mensageria habilitada (ver {@link MensageriaConfig}); os prazos vêm de {@code saga.*.expiracao}.
 */
public class ExpiracaoSagaAgendador {

    private static final Logger log = LoggerFactory.getLogger(ExpiracaoSagaAgendador.class);

    private final ExpirarOrcamentosUseCase expirarOrcamentos;
    private final ExpirarCobrancasUseCase expirarCobrancas;
    private final TransactionOperations transacao;

    public ExpiracaoSagaAgendador(ExpirarOrcamentosUseCase expirarOrcamentos,
                                  ExpirarCobrancasUseCase expirarCobrancas,
                                  TransactionOperations transacao) {
        this.expirarOrcamentos = expirarOrcamentos;
        this.expirarCobrancas = expirarCobrancas;
        this.transacao = transacao;
    }

    @Scheduled(fixedDelayString = "${saga.expiracao.intervalo-ms:60000}")
    public void verificar() {
        Integer orcamentos = transacao.execute(status -> expirarOrcamentos.executar());
        Integer cobrancas = transacao.execute(status -> expirarCobrancas.executar());
        if (orcamentos != null && orcamentos > 0 || cobrancas != null && cobrancas > 0) {
            log.info("[SAGA_TIMEOUT] orcamentos_expirados={} cobrancas_expiradas={}", orcamentos, cobrancas);
        }
    }
}
