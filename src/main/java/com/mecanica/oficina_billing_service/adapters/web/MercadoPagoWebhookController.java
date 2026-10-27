package com.mecanica.oficina_billing_service.adapters.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanica.oficina_billing_service.adapters.mercadopago.MercadoPagoWebhookValidator;
import com.mecanica.oficina_billing_service.adapters.web.dto.request.MercadoPagoWebhookRequest;
import com.mecanica.oficina_billing_service.application.cobranca.exception.InvalidWebhookSignatureException;
import com.mecanica.oficina_billing_service.application.cobranca.input.ProcessarWebhookInput;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ProcessarWebhookMercadoPagoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint de Webhook para recepção de notificações assíncronas do Mercado Pago com validação de segurança HMAC.
 */
@RestController
@RequestMapping("/webhooks/mercadopago")
@Tag(name = "Webhooks", description = "Recebimento de notificações assíncronas do Mercado Pago")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final ProcessarWebhookMercadoPagoUseCase processarWebhookUseCase;
    private final MercadoPagoWebhookValidator webhookValidator;
    private final ObjectMapper objectMapper;

    public MercadoPagoWebhookController(ProcessarWebhookMercadoPagoUseCase processarWebhookUseCase,
                                        MercadoPagoWebhookValidator webhookValidator,
                                        @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.processarWebhookUseCase = processarWebhookUseCase;
        this.webhookValidator = webhookValidator;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @PostMapping
    @Operation(summary = "Receber webhook do Mercado Pago", description = "Valida a assinatura x-signature HMAC e processa atualizações de status de pagamento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificação recebida e processada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Assinatura do webhook inválida ou ausente"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar notificação")
    })
    public ResponseEntity<String> receber(
            @RequestHeader(name = "x-signature", required = false) String xSignature,
            @RequestHeader(name = "x-request-id", required = false) String xRequestId,
            @RequestParam(name = "id", required = false) String paramId,
            @RequestParam(name = "topic", required = false) String paramTopic,
            @RequestParam(name = "data.id", required = false) String paramDataId,
            @RequestBody(required = false) String payload) {

        log.info("Recebido webhook do Mercado Pago. Query: id={}, topic={}, data.id={}. x-signature presente: {}",
                paramId, paramTopic, paramDataId, (xSignature != null && !xSignature.isBlank()));

        String dataId = paramDataId != null ? paramDataId : paramId;
        String action = null;
        String type = paramTopic;

        if (payload != null && !payload.isBlank()) {
            try {
                var request = objectMapper.readValue(payload, MercadoPagoWebhookRequest.class);
                if (request.getData() != null && request.getData().getId() != null) {
                    dataId = request.getData().getId();
                } else if (request.getId() != null) {
                    dataId = request.getId();
                }
                action = request.getAction();
                type = request.getType() != null ? request.getType() : type;
            } catch (Exception e) {
                log.warn("Payload do webhook não pôde ser desserializado como MercadoPagoWebhookRequest: {}", e.getMessage());
            }
        }

        // Validação de segurança criptográfica (HMAC-SHA256)
        try {
            if (webhookValidator != null) {
                webhookValidator.validarAssinatura(xSignature, xRequestId, dataId);
            }
        } catch (InvalidWebhookSignatureException e) {
            log.error("Rejeitando webhook não autenticado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature: " + e.getMessage());
        }

        // Processa o evento de forma assíncrona/idempotente
        if (dataId != null && !dataId.isBlank()) {
            processarWebhookUseCase.executar(new ProcessarWebhookInput(action, type, dataId));
        } else {
            log.warn("Notificação descartada: nenhum identificador de recurso localizado nos parâmetros ou corpo");
        }

        // Responde 200 OK rapidamente dentro do timeout de 22s estipulado pelo Mercado Pago
        return ResponseEntity.ok("ok");
    }
}
