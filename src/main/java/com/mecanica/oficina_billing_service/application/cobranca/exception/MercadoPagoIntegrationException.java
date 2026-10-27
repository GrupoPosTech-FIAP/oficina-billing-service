package com.mecanica.oficina_billing_service.application.cobranca.exception;

/**
 * Exceção lançada quando ocorre falha na comunicação ou processamento das APIs do Mercado Pago.
 */
public class MercadoPagoIntegrationException extends RuntimeException {

    public MercadoPagoIntegrationException(String message) {
        super(message);
    }

    public MercadoPagoIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
