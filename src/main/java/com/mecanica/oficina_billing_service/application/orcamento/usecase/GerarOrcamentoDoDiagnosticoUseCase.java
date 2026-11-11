package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.input.GerarOrcamentoInput;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;

import java.util.Optional;
import java.util.Set;

/**
 * Passo da Saga disparado por {@code DiagnosticoConcluido}: gera o orçamento e já o envia ao
 * cliente (o envio publica {@code OrcamentoGerado}).
 *
 * <p>Se a OS já tem um orçamento em andamento, não faz nada — o diagnóstico foi reenviado.
 */
public class GerarOrcamentoDoDiagnosticoUseCase {

    private static final Set<OrcamentoStatus> ENCERRADOS = Set.of(OrcamentoStatus.NEGADO, OrcamentoStatus.EXPIRADO);

    private final OrcamentoGateway orcamentoGateway;
    private final GerarOrcamentoUseCase gerarOrcamentoUseCase;
    private final EnviarOrcamentoUseCase enviarOrcamentoUseCase;

    public GerarOrcamentoDoDiagnosticoUseCase(OrcamentoGateway orcamentoGateway,
                                              GerarOrcamentoUseCase gerarOrcamentoUseCase,
                                              EnviarOrcamentoUseCase enviarOrcamentoUseCase) {
        this.orcamentoGateway = orcamentoGateway;
        this.gerarOrcamentoUseCase = gerarOrcamentoUseCase;
        this.enviarOrcamentoUseCase = enviarOrcamentoUseCase;
    }

    public Optional<Orcamento> executar(GerarOrcamentoInput input) {
        boolean emAndamento = orcamentoGateway.buscarPorOrdemServicoId(input.ordemServicoId())
                .filter(existente -> !ENCERRADOS.contains(existente.getStatus()))
                .isPresent();
        if (emAndamento) {
            return Optional.empty();
        }

        Orcamento gerado = gerarOrcamentoUseCase.executar(input);
        return Optional.of(enviarOrcamentoUseCase.executar(gerado.getId()));
    }
}
