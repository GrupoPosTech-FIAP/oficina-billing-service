package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.cobranca.input.ProcessarWebhookInput;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caso de uso responsável pelo processamento assíncrono de notificações de Webhook do Mercado Pago.
 */
public class ProcessarWebhookMercadoPagoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarWebhookMercadoPagoUseCase.class);

    private final CobrancaGateway cobrancaGateway;
    private final MercadoPagoGateway mercadoPagoGateway;

    public ProcessarWebhookMercadoPagoUseCase(CobrancaGateway cobrancaGateway, MercadoPagoGateway mercadoPagoGateway) {
        this.cobrancaGateway = cobrancaGateway;
        this.mercadoPagoGateway = mercadoPagoGateway;
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
                    if (cobranca.getStatus() == CobrancaStatus.PAGO) {
                        log.info("Cobrança {} já se encontra como PAGO. Ignorando evento repetido.", cobranca.getId());
                    } else {
                        log.info("Atualizando cobrança {} para PAGO.", cobranca.getId());
                        cobrancaGateway.atualizar(cobranca.marcarComoPago());
                    }
                }
                case "rejected", "cancelled" -> {
                    if (cobranca.getStatus() == CobrancaStatus.RECUSADO || cobranca.getStatus() == CobrancaStatus.CANCELADO) {
                        log.info("Cobrança {} já se encontra como RECUSADA/CANCELADA.", cobranca.getId());
                    } else {
                        log.info("Atualizando cobrança {} para RECUSADO.", cobranca.getId());
                        cobrancaGateway.atualizar(cobranca.marcarComoRecusado());
                    }
                }
                case "refunded", "charged_back" -> {
                    if (cobranca.getStatus() == CobrancaStatus.ESTORNADO) {
                        log.info("Cobrança {} já se encontra como ESTORNADA.", cobranca.getId());
                    } else if (cobranca.getStatus() == CobrancaStatus.PAGO) {
                        log.info("Estornando cobrança {}", cobranca.getId());
                        cobrancaGateway.atualizar(cobranca.estornar());
                    } else {
                        log.warn("Tentativa de estorno em cobrança não paga: status atual {}", cobranca.getStatus());
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
}
