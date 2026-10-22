package com.mecanica.oficina_billing_service.adapters.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanica.oficina_billing_service.adapters.web.dto.request.MercadoPagoWebhookRequest;
import com.mecanica.oficina_billing_service.application.cobranca.input.ProcessarWebhookInput;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ProcessarWebhookMercadoPagoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/mercadopago")
@Tag(name = "Webhooks", description = "Recebimento de notificações assíncronas do Mercado Pago")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final ProcessarWebhookMercadoPagoUseCase processarWebhookUseCase;
    private final ObjectMapper objectMapper;

    public MercadoPagoWebhookController(ProcessarWebhookMercadoPagoUseCase processarWebhookUseCase,
                                        @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.processarWebhookUseCase = processarWebhookUseCase;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @PostMapping
    @Operation(summary = "Receber webhook do Mercado Pago", description = "Endpoint público chamado pelo Mercado Pago para atualizar status de pagamentos")
    public ResponseEntity<String> receber(
            @RequestBody(required = false) String payload,
            @RequestParam(name = "id", required = false) String paramId,
            @RequestParam(name = "topic", required = false) String paramTopic) {

        log.info("Recebido webhook do Mercado Pago. Query: id={}, topic={}. Payload: {}", paramId, paramTopic, payload);

        String dataId = paramId;
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

        if (dataId != null) {
            processarWebhookUseCase.executar(new ProcessarWebhookInput(action, type, dataId));
        }

        return ResponseEntity.ok("ok");
    }
}
