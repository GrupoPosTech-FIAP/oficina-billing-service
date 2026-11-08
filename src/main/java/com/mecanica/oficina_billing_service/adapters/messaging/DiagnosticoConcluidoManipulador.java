package com.mecanica.oficina_billing_service.adapters.messaging;

import com.mecanica.oficina_billing_service.application.evento.EventosSaga;
import com.mecanica.oficina_billing_service.application.orcamento.input.GerarOrcamentoInput;
import com.mecanica.oficina_billing_service.application.orcamento.input.ItemInsumoInput;
import com.mecanica.oficina_billing_service.application.orcamento.input.ItemServicoInput;
import com.mecanica.oficina_billing_service.application.orcamento.usecase.GerarOrcamentoDoDiagnosticoUseCase;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** {@code DiagnosticoConcluido} → gera e envia o orçamento. */
@Component
public class DiagnosticoConcluidoManipulador implements ManipuladorEvento {

    private final GerarOrcamentoDoDiagnosticoUseCase useCase;

    public DiagnosticoConcluidoManipulador(GerarOrcamentoDoDiagnosticoUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public Set<String> eventTypes() {
        return Set.of(EventosSaga.DIAGNOSTICO_CONCLUIDO);
    }

    @Override
    public void tratar(EventoEnvelope evento) {
        JsonNode payload = evento.payload();

        List<ItemServicoInput> servicos = new ArrayList<>();
        for (JsonNode s : payload.path("servicos")) {
            servicos.add(new ItemServicoInput(s.path("servicoId").asString(null), s.path("quantidade").asInt(1),
                    decimal(s.get("precoUnitario")), s.path("descricao").asString(null)));
        }

        List<ItemInsumoInput> insumos = new ArrayList<>();
        for (JsonNode i : payload.path("insumos")) {
            insumos.add(new ItemInsumoInput(i.path("insumoId").asString(null), i.path("quantidade").asInt(),
                    decimal(i.get("precoUnitario")), i.path("descricao").asString(null)));
        }

        useCase.executar(new GerarOrcamentoInput(evento.sagaId(), payload.path("clienteId").asString(null),
                insumos, servicos, payload.path("observacoes").asString(null)));
    }

    private static BigDecimal decimal(JsonNode valor) {
        return valor == null || valor.isNull() ? null : valor.decimalValue();
    }
}
