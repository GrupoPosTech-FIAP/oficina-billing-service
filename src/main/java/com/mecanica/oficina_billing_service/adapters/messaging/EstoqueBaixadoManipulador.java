package com.mecanica.oficina_billing_service.adapters.messaging;

import com.mecanica.oficina_billing_service.application.cobranca.usecase.CobrarOrdemServicoUseCase;
import com.mecanica.oficina_billing_service.application.evento.EventosSaga;
import java.util.Set;
import org.springframework.stereotype.Component;

/** {@code EstoqueBaixado} → cria a cobrança no Mercado Pago. */
@Component
public class EstoqueBaixadoManipulador implements ManipuladorEvento {

    private final CobrarOrdemServicoUseCase useCase;

    public EstoqueBaixadoManipulador(CobrarOrdemServicoUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public Set<String> eventTypes() {
        return Set.of(EventosSaga.ESTOQUE_BAIXADO);
    }

    @Override
    public void tratar(EventoEnvelope evento) {
        useCase.executar(evento.sagaId());
    }
}
