package com.mecanica.oficina_billing_service.adapters.messaging;

import com.mecanica.oficina_billing_service.application.cobranca.usecase.ExpirarCobrancasUseCase;
import com.mecanica.oficina_billing_service.application.orcamento.usecase.ExpirarOrcamentosUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpiracaoSagaAgendadorTest {

    @Test
    void verificaOrcamentosECobrancas() {
        ExpirarOrcamentosUseCase orcamentos = mock(ExpirarOrcamentosUseCase.class);
        ExpirarCobrancasUseCase cobrancas = mock(ExpirarCobrancasUseCase.class);
        when(orcamentos.executar()).thenReturn(1);

        new ExpiracaoSagaAgendador(orcamentos, cobrancas, TransactionOperations.withoutTransaction()).verificar();

        verify(orcamentos).executar();
        verify(cobrancas).executar();
    }
}
