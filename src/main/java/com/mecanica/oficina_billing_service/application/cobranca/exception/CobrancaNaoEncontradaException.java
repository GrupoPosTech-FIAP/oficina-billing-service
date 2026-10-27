package com.mecanica.oficina_billing_service.application.cobranca.exception;

/**
 * Exceção lançada quando uma cobrança solicitada não é localizada no repositório.
 */
public class CobrancaNaoEncontradaException extends RuntimeException {

    public CobrancaNaoEncontradaException(String message) {
        super(message);
    }
}
