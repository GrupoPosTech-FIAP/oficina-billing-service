package com.mecanica.oficina_billing_service.adapters.messaging;

import com.mecanica.oficina_billing_service.application.cobranca.usecase.EstornarPagamentoOrdemServicoUseCase;
import com.mecanica.oficina_billing_service.application.evento.EventosSaga;
import java.util.Set;
import org.springframework.stereotype.Component;

/** {@code ExecucaoFalhou} → reembolsa o pagamento da OS. */
@Component
public class ExecucaoFalhouManipulador implements ManipuladorEvento {

    private final EstornarPagamentoOrdemServicoUseCase useCase;

    public ExecucaoFalhouManipulador(EstornarPagamentoOrdemServicoUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public Set<String> eventTypes() {
        return Set.of(EventosSaga.EXECUCAO_FALHOU);
    }

    @Override
    public void tratar(EventoEnvelope evento) {
        useCase.executar(evento.sagaId());
    }
}
