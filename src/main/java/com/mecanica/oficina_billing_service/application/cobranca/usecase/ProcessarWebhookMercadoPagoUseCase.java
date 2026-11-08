package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.cobranca.input.ProcessarWebhookInput;
import com.mecanica.oficina_billing_service.application.evento.EventosSaga;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Caso de uso responsável pelo processamento assíncrono de notificações de Webhook do Mercado Pago.
 *
 * <p>Na Saga, a confirmação publica {@code PagamentoConfirmado} e a recusa publica
 * {@code PagamentoRecusado} — apenas na primeira notificação que muda o status da cobrança.
 * Pagamento aprovado para cobrança que já não aceita pagamento (expirada, recusada ou já paga por
 * outro pagamento) é reembolsado e publica {@code PagamentoEstornado}.
 */
public class ProcessarWebhookMercadoPagoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarWebhookMercadoPagoUseCase.class);

    private final CobrancaGateway cobrancaGateway;
    private final MercadoPagoGateway mercadoPagoGateway;
    private final PublicadorEventosGateway publicador;

    public ProcessarWebhookMercadoPagoUseCase(CobrancaGateway cobrancaGateway, MercadoPagoGateway mercadoPagoGateway,
                                              PublicadorEventosGateway publicador) {
        this.cobrancaGateway = cobrancaGateway;
        this.mercadoPagoGateway = mercadoPagoGateway;
        this.publicador = publicador;
    }

    public void executar(ProcessarWebhookInput input) {
        if (input == null || input.dataId() == null || input.dataId().isBlank()) {
            log.warn("Webhook do Mercado Pago recebido sem identificador de dados (data.id)");
            return;
        }

        try {
            Long paymentId = Long.parseLong(input.dataId().trim());
            var detalheOpt = mercadoPagoGateway.consultarPagamento(paymentId);

            if (detalheOpt.isEmpty()) {
                log.warn("Pagamento não encontrado na API do Mercado Pago para paymentId: {}", paymentId);
                return;
            }

            var detalhe = detalheOpt.get();
            String externalReference = detalhe.externalReference();

            if (externalReference == null || externalReference.isBlank()) {
                log.warn("Pagamento {} não possui externalReference associada", paymentId);
                return;
            }

            var cobrancaOpt = cobrancaGateway.buscarPorReferencia(externalReference);
            if (cobrancaOpt.isEmpty()) {
                log.warn("Nenhuma cobrança encontrada para a referência externa: {}", externalReference);
                return;
            }

            Cobranca cobranca = cobrancaOpt.get();
            String status = detalhe.status() != null ? detalhe.status().toLowerCase() : "";
            log.info("Processando webhook para cobrança ID {}, ref: {}, status Mercado Pago: {}",
                    cobranca.getId(), externalReference, status);

            switch (status) {
                case "approved" -> {
                    if (cobranca.aceitaPagamento()) {
                        log.info("Atualizando cobrança {} para PAGO.", cobranca.getId());
                        Cobranca paga = cobrancaGateway.atualizar(cobranca.marcarComoPago(paymentId));
                        publicador.publicar(EventosSaga.PAGAMENTO_CONFIRMADO, paga.getPedidoId(), Map.of(
                                "ordemServicoId", paga.getPedidoId(),
                                "cobrancaId", paga.getId().toString(),
                                "mercadoPagoPaymentId", paymentId,
                                "valor", paga.getValor()));
                    } else if (paymentId.equals(cobranca.getPagamentoId())
                            // paga antes de a cobrança guardar o id do pagamento: não dá para saber se é outro
                            || (cobranca.getStatus() == CobrancaStatus.PAGO && cobranca.getPagamentoId() == null)) {
                        log.info("Pagamento {} já registrado na cobrança {} ({}). Ignorando evento repetido.",
                                paymentId, cobranca.getId(), cobranca.getStatus());
                    } else {
                        devolverPagamentoNaoAceito(cobranca, paymentId);
                    }
                }
                case "rejected", "cancelled" -> {
                    if (cobranca.getStatus() == CobrancaStatus.RECUSADO || cobranca.getStatus() == CobrancaStatus.CANCELADO) {
                        log.info("Cobrança {} já se encontra como RECUSADA/CANCELADA.", cobranca.getId());
                    } else {
                        log.info("Atualizando cobrança {} para RECUSADO.", cobranca.getId());
                        Cobranca recusada = cobrancaGateway.atualizar(cobranca.marcarComoRecusado());
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("ordemServicoId", recusada.getPedidoId());
                        payload.put("cobrancaId", recusada.getId().toString());
                        payload.put("mercadoPagoPaymentId", paymentId);
                        payload.put("motivo", detalhe.statusDetail() != null ? detalhe.statusDetail() : status);
                        publicador.publicar(EventosSaga.PAGAMENTO_RECUSADO, recusada.getPedidoId(), payload);
                    }
                }
                case "refunded", "charged_back" -> {
                    if (cobranca.getStatus() == CobrancaStatus.ESTORNADO) {
                        log.info("Cobrança {} já se encontra como ESTORNADA.", cobranca.getId());
                    } else if (cobranca.getStatus() == CobrancaStatus.PAGO
                            && (cobranca.getPagamentoId() == null || cobranca.getPagamentoId().equals(paymentId))) {
                        log.info("Estornando cobrança {}", cobranca.getId());
                        cobrancaGateway.atualizar(cobranca.estornar());
                    } else {
                        log.info("Estorno do pagamento {} não altera a cobrança {} ({})",
                                paymentId, cobranca.getId(), cobranca.getStatus());
                    }
                }
                case "in_process", "pending" -> {
                    log.info("Pagamento {} em processamento/pendente. Cobrança mantida em {}", paymentId, cobranca.getStatus());
                }
                default -> {
                    log.info("Status não mapeado do Mercado Pago: {}. Nenhuma ação executada na cobrança {}.", status, cobranca.getId());
                }
            }
        } catch (NumberFormatException e) {
            log.warn("data.id '{}' não é um identificador numérico de pagamento", input.dataId());
        } catch (Exception e) {
            log.error("Erro ao processar webhook do Mercado Pago: {}", e.getMessage(), e);
            throw new RuntimeException("Falha no processamento de webhook do Mercado Pago", e);
        }
    }

    /**
     * Pagamento aprovado que a cobrança não aceita mais: chegou depois de a cobrança expirar ou ser
     * recusada (a OS já foi cancelada e o estoque devolvido) ou é um segundo pagamento da mesma
     * cobrança. É reembolsado no Mercado Pago e publica {@code PagamentoEstornado}, que entra no
     * histórico da OS.
     *
     * <p>O reembolso vem antes da gravação. Se a gravação falhar, o Mercado Pago reenvia o webhook e
     * o reembolso é repetido com a mesma chave de idempotência, sem devolver duas vezes.
     */
    private void devolverPagamentoNaoAceito(Cobranca cobranca, Long paymentId) {
        String motivo = switch (cobranca.getStatus()) {
            case CANCELADO -> "PAGAMENTO_APOS_EXPIRACAO";
            case RECUSADO -> "PAGAMENTO_APOS_RECUSA";
            default -> "PAGAMENTO_DUPLICADO";
        };
        log.warn("Pagamento {} aprovado para a cobrança {} em {}: reembolsando ({})",
                paymentId, cobranca.getId(), cobranca.getStatus(), motivo);

        mercadoPagoGateway.estornarPagamento(paymentId);
        if (!motivo.equals("PAGAMENTO_DUPLICADO")) {
            // pagamento duplicado não muda a cobrança: ela continua com o pagamento que valeu
            cobrancaGateway.atualizar(cobranca.estornarPagamentoTardio(paymentId));
        }

        publicador.publicar(EventosSaga.PAGAMENTO_ESTORNADO, cobranca.getPedidoId(), Map.of(
                "ordemServicoId", cobranca.getPedidoId(),
                "cobrancaId", cobranca.getId().toString(),
                "mercadoPagoPaymentId", paymentId,
                "valor", cobranca.getValor(),
                "motivo", motivo));
    }
}
