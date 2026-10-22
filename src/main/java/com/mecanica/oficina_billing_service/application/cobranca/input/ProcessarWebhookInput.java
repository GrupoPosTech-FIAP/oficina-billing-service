package com.mecanica.oficina_billing_service.application.cobranca.input;

public record ProcessarWebhookInput(
        String action,
        String type,
        String dataId
) {}
