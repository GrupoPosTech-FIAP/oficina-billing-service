package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.evento.EventosSaga;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * Timeout da Saga: orçamento enviado e sem resposta do cliente dentro do prazo vira
 * {@code EXPIRADO} e publica {@code OrcamentoExpirado} (a OS é cancelada).
 */
public class ExpirarOrcamentosUseCase {

    private final OrcamentoGateway orcamentoGateway;
    private final PublicadorEventosGateway publicador;
    private final Duration prazo;
    private final Clock clock;

    public ExpirarOrcamentosUseCase(OrcamentoGateway orcamentoGateway, PublicadorEventosGateway publicador,
                                    Duration prazo, Clock clock) {
        this.orcamentoGateway = orcamentoGateway;
        this.publicador = publicador;
        this.prazo = prazo;
        this.clock = clock;
    }

    /** @return quantidade de orçamentos expirados */
    public int executar() {
        // criadoEm é gravado com LocalDateTime.now(), no fuso da JVM
        LocalDateTime limite = LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault()).minus(prazo);

        List<Orcamento> vencidos = orcamentoGateway
                .listarPorStatus(List.of(OrcamentoStatus.ENVIADO, OrcamentoStatus.AGUARDANDO)).stream()
                .filter(o -> o.getCriadoEm().isBefore(limite))
                .toList();

        for (Orcamento orcamento : vencidos) {
            Orcamento expirado = orcamentoGateway.atualizar(orcamento.expirar());
            publicador.publicar(EventosSaga.ORCAMENTO_EXPIRADO, expirado.getOrdemServicoId(), Map.of(
                    "ordemServicoId", expirado.getOrdemServicoId(),
                    "orcamentoId", expirado.getId()));
        }
        return vencidos.size();
    }
}
