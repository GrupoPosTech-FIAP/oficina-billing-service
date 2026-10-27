package com.mecanica.oficina_billing_service.adapters.mercadopago;

import com.mecanica.oficina_billing_service.adapters.config.MercadoPagoProperties;
import com.mecanica.oficina_billing_service.application.cobranca.exception.InvalidWebhookSignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MercadoPagoWebhookValidatorTest {

    private MercadoPagoProperties properties;
    private MercadoPagoWebhookValidator validator;
    private final String secret = "test_webhook_secret_key_12345";

    @BeforeEach
    void setUp() {
        properties = new MercadoPagoProperties();
        properties.setWebhookSecret(secret);
        validator = new MercadoPagoWebhookValidator(properties);
    }

    private String generateSignature(String manifest, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Test
    @DisplayName("Deve validar com sucesso quando a assinatura HMAC-SHA256 estiver correta")
    void deveValidarAssinaturaCorreta() throws Exception {
        String dataId = "99887766";
        String requestId = "req-uuid-1234-5678";
        long ts = Instant.now().getEpochSecond();

        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        String v1 = generateSignature(manifest, secret);
        String xSignature = "ts=" + ts + ",v1=" + v1;

        assertDoesNotThrow(() -> validator.validarAssinatura(xSignature, requestId, dataId));
    }

    @Test
    @DisplayName("Deve lançar exceção quando a assinatura HMAC-SHA256 for inválida")
    void deveLancarExcecaoQuandoAssinaturaInvalida() {
        String dataId = "99887766";
        String requestId = "req-uuid-1234-5678";
        long ts = Instant.now().getEpochSecond();
        String xSignature = "ts=" + ts + ",v1=invalid_hash_value_1234567890abcdef";

        assertThrows(InvalidWebhookSignatureException.class, () ->
                validator.validarAssinatura(xSignature, requestId, dataId)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando o timestamp do webhook tiver expirado (replay attack)")
    void deveLancarExcecaoQuandoTimestampExpirado() throws Exception {
        String dataId = "99887766";
        String requestId = "req-uuid-1234-5678";
        // 10 minutos no passado (ultrapassa a tolerância de 5 min)
        long ts = Instant.now().getEpochSecond() - 600;

        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        String v1 = generateSignature(manifest, secret);
        String xSignature = "ts=" + ts + ",v1=" + v1;

        assertThrows(InvalidWebhookSignatureException.class, () ->
                validator.validarAssinatura(xSignature, requestId, dataId)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando x-signature estiver ausente e secret estiver configurado")
    void deveLancarExcecaoQuandoHeaderAusente() {
        assertThrows(InvalidWebhookSignatureException.class, () ->
                validator.validarAssinatura(null, "req-1", "123")
        );
    }

    @Test
    @DisplayName("Deve ignorar validação se webhookSecret não estiver configurado (modo local dev)")
    void deveIgnorarValidacaoSeSecretVazio() {
        properties.setWebhookSecret("");
        validator = new MercadoPagoWebhookValidator(properties);

        assertDoesNotThrow(() -> validator.validarAssinatura(null, null, "123"));
    }
}
