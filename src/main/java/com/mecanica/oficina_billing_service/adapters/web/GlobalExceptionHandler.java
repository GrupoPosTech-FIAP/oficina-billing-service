package com.mecanica.oficina_billing_service.adapters.web;

import com.mecanica.oficina_billing_service.application.cobranca.exception.CobrancaNaoEncontradaException;
import com.mecanica.oficina_billing_service.application.cobranca.exception.InvalidWebhookSignatureException;
import com.mecanica.oficina_billing_service.application.cobranca.exception.MercadoPagoIntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Tratamento global de exceções da camada Web com respostas padronizadas e códigos de estado HTTP semânticos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(
            int status,
            String error,
            String message,
            OffsetDateTime timestamp,
            Map<String, String> details
    ) {
        public ErrorResponse(int status, String error, String message) {
            this(status, error, message, OffsetDateTime.now(), null);
        }

        public ErrorResponse(int status, String error, String message, Map<String, String> details) {
            this(status, error, message, OffsetDateTime.now(), details);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        var error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Dados de entrada inválidos",
                "Um ou mais campos obrigatórios não foram preenchidos corretamente.",
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Requisição com argumento inválido: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Argumento inválido", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(CobrancaNaoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(CobrancaNaoEncontradaException ex) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Não encontrado", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidWebhookSignatureException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSignature(InvalidWebhookSignatureException ex) {
        log.error("Assinatura de webhook não autorizada: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Não autorizado", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Estado inválido para a operação: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Operação não permitida", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(MercadoPagoIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleMercadoPagoIntegration(MercadoPagoIntegrationException ex) {
        log.error("Falha na integração com Mercado Pago: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_GATEWAY.value(), "Erro no Gateway de Pagamento", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Erro interno não tratado: {}", ex.getMessage(), ex);
        var error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno", "Ocorreu um erro interno no servidor.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
