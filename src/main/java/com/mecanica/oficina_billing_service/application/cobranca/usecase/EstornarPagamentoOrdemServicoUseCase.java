package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.evento.EventosSaga;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;

import java.util.Map;
import java.util.Optional;

/**
 * Compensação da Saga disparada por {@code ExecucaoFalhou}: reembolsa o pagamento no Mercado Pago,
 * marca a cobrança como {@code ESTORNADO} e publica {@code PagamentoEstornado}.
 *
 * <p>Cobrança inexistente ou que não está paga não tem o que estornar. Se o reembolso falhar, a
 * exceção devolve a mensagem para a fila; esgotadas as tentativas ela vai para a DLQ.
 */
public class EstornarPagamentoOrdemServicoUseCase {

    private final CobrancaGateway cobrancaGateway;
    private final MercadoPagoGateway mercadoPagoGateway;
    private final PublicadorEventosGateway publicador;

    public EstornarPagamentoOrdemServicoUseCase(CobrancaGateway cobrancaGateway,
                                                MercadoPagoGateway mercadoPagoGateway,
                                                PublicadorEventosGateway publicador) {
        this.cobrancaGateway = cobrancaGateway;
        this.mercadoPagoGateway = mercadoPagoGateway;
        this.publicador = publicador;
    }

    /** @return {@code true} se houve estorno */
    public boolean executar(String ordemServicoId) {
        Optional<Cobranca> registro = cobrancaGateway.buscarPorPedidoId(ordemServicoId);
        if (registro.isEmpty() || registro.get().getStatus() != CobrancaStatus.PAGO) {
            return false;
        }

        Cobranca cobranca = registro.get();
        if (cobranca.getPagamentoId() == null) {
            throw new IllegalStateException("Cobrança " + cobranca.getId() + " está paga mas sem id de pagamento do Mercado Pago");
        }

        mercadoPagoGateway.estornarPagamento(cobranca.getPagamentoId());
        Cobranca estornada = cobrancaGateway.atualizar(cobranca.estornar());

        publicador.publicar(EventosSaga.PAGAMENTO_ESTORNADO, ordemServicoId, Map.of(
                "ordemServicoId", ordemServicoId,
                "cobrancaId", estornada.getId().toString(),
                "mercadoPagoPaymentId", estornada.getPagamentoId(),
                "valor", estornada.getValor()));
        return true;
    }
}
