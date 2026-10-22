package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.cobranca.input.ProcessarWebhookInput;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessarWebhookMercadoPagoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarWebhookMercadoPagoUseCase.class);

    private final CobrancaGateway cobrancaGateway;
    private final MercadoPagoGateway mercadoPagoGateway;

    public ProcessarWebhookMercadoPagoUseCase(CobrancaGateway cobrancaGateway, MercadoPagoGateway mercadoPagoGateway) {
        this.cobrancaGateway = cobrancaGateway;
        this.mercadoPagoGateway = mercadoPagoGateway;
    }

    public void executar(ProcessarWebhookInput input) {
        if (input == null || input.dataId() == null) {
            log.warn("Webhook do Mercado Pago recebido sem data.id");
            return;
        }

        try {
            Long paymentId = Long.parseLong(input.dataId());
            var detalheOpt = mercadoPagoGateway.consultarPagamento(paymentId);

            if (detalheOpt.isEmpty()) {
                log.warn("Pagamento não encontrado na API do Mercado Pago para paymentId: {}", paymentId);
                return;
            }

            var detalhe = detalheOpt.get();
            String externalReference = detalhe.externalReference();

            if (externalReference == null) {
                log.warn("Pagamento {} não possui externalReference associada", paymentId);
                return;
            }

            var cobrancaOpt = cobrancaGateway.buscarPorReferencia(externalReference);
            if (cobrancaOpt.isEmpty()) {
                log.warn("Nenhuma cobrança encontrada com a referência: {}", externalReference);
                return;
            }

            Cobranca cobranca = cobrancaOpt.get();
            String status = detalhe.status();

            if ("approved".equalsIgnoreCase(status)) {
                log.info("Pagamento aprovado no Mercado Pago para referência {}. Atualizando cobrança para PAGO.", externalReference);
                cobrancaGateway.atualizar(cobranca.marcarComoPago());
            } else if ("rejected".equalsIgnoreCase(status)) {
                log.info("Pagamento rejeitado no Mercado Pago para referência {}. Atualizando cobrança para RECUSADO.", externalReference);
                cobrancaGateway.atualizar(cobranca.marcarComoRecusado());
            } else {
                log.info("Status do pagamento {} no Mercado Pago: {}. Nenhuma transição requerida.", paymentId, status);
            }
        } catch (NumberFormatException e) {
            log.warn("data.id {} não é um ID numérico de pagamento", input.dataId());
        } catch (Exception e) {
            log.error("Erro ao processar webhook do Mercado Pago: {}", e.getMessage(), e);
            throw new RuntimeException("Falha no processamento de webhook do Mercado Pago", e);
        }
    }
}
