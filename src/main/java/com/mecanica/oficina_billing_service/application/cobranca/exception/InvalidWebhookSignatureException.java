package com.mecanica.oficina_billing_service.application.cobranca.exception;

/**
 * Exceção lançada quando a validação do cabeçalho x-signature do Mercado Pago falha.
 */
public class InvalidWebhookSignatureException extends RuntimeException {

    public InvalidWebhookSignatureException(String message) {
        super(message);
    }
}
