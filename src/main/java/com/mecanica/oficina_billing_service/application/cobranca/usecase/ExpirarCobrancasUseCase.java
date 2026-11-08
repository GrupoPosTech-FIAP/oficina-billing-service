package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.evento.EventosSaga;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Timeout da Saga: cobrança {@code PENDENTE} além do prazo é cancelada e publica
 * {@code PagamentoExpirado} (o estoque é estornado e a OS cancelada).
 */
public class ExpirarCobrancasUseCase {

    private final CobrancaGateway cobrancaGateway;
    private final PublicadorEventosGateway publicador;
    private final Duration prazo;
    private final Clock clock;

    public ExpirarCobrancasUseCase(CobrancaGateway cobrancaGateway, PublicadorEventosGateway publicador,
                                   Duration prazo, Clock clock) {
        this.cobrancaGateway = cobrancaGateway;
        this.publicador = publicador;
        this.prazo = prazo;
        this.clock = clock;
    }

    /** @return quantidade de cobranças expiradas */
    public int executar() {
        OffsetDateTime limite = OffsetDateTime.now(clock).minus(prazo);

        List<Cobranca> vencidas = cobrancaGateway.listarPorStatus(CobrancaStatus.PENDENTE).stream()
                .filter(c -> c.getCreatedAt().isBefore(limite))
                .toList();

        for (Cobranca cobranca : vencidas) {
            Cobranca cancelada = cobrancaGateway.atualizar(cobranca.cancelar());
            publicador.publicar(EventosSaga.PAGAMENTO_EXPIRADO, cancelada.getPedidoId(), Map.of(
                    "ordemServicoId", cancelada.getPedidoId(),
                    "cobrancaId", cancelada.getId().toString()));
        }
        return vencidas.size();
    }
}
