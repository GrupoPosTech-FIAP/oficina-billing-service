package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.input.CriarCobrancaInput;
import com.mecanica.oficina_billing_service.application.cobranca.output.CobrancaOutput;
import com.mecanica.oficina_billing_service.application.evento.EventosSaga;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;

import java.util.Map;
import java.util.Optional;

/**
 * Passo da Saga disparado por {@code EstoqueBaixado}: cria a cobrança do orçamento aprovado no
 * Checkout Pro e publica {@code CobrancaCriada} com o link de pagamento.
 *
 * <p>A cobrança usa o id da OS como {@code pedidoId}; se já existir, não faz nada.
 */
public class CobrarOrdemServicoUseCase {

    private final OrcamentoGateway orcamentoGateway;
    private final CobrancaGateway cobrancaGateway;
    private final CriarCobrancaUseCase criarCobrancaUseCase;
    private final PublicadorEventosGateway publicador;

    public CobrarOrdemServicoUseCase(OrcamentoGateway orcamentoGateway,
                                     CobrancaGateway cobrancaGateway,
                                     CriarCobrancaUseCase criarCobrancaUseCase,
                                     PublicadorEventosGateway publicador) {
        this.orcamentoGateway = orcamentoGateway;
        this.cobrancaGateway = cobrancaGateway;
        this.criarCobrancaUseCase = criarCobrancaUseCase;
        this.publicador = publicador;
    }

    public Optional<CobrancaOutput> executar(String ordemServicoId) {
        if (cobrancaGateway.buscarPorPedidoId(ordemServicoId).isPresent()) {
            return Optional.empty();
        }

        Orcamento orcamento = orcamentoGateway.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new IllegalStateException("Ordem de serviço sem orçamento: " + ordemServicoId));
        if (orcamento.getStatus() != OrcamentoStatus.APROVADO) {
            throw new IllegalStateException("Orçamento " + orcamento.getId() + " não está APROVADO (status "
                    + orcamento.getStatus() + ")");
        }

        CobrancaOutput cobranca = criarCobrancaUseCase.executar(new CriarCobrancaInput(
                ordemServicoId, "Ordem de serviço " + ordemServicoId, orcamento.getValorTotal()));

        publicador.publicar(EventosSaga.COBRANCA_CRIADA, ordemServicoId, Map.of(
                "ordemServicoId", ordemServicoId,
                "cobrancaId", cobranca.id().toString(),
                "valor", cobranca.valor(),
                "checkoutUrl", cobranca.initPoint()));
        return Optional.of(cobranca);
    }
}
