package com.mecanica.oficina_billing_service.adapters.mercadopago;

import com.mecanica.oficina_billing_service.adapters.config.MercadoPagoProperties;
import com.mecanica.oficina_billing_service.application.cobranca.exception.InvalidWebhookSignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Validador de assinaturas HMAC-SHA256 para Webhooks do Mercado Pago (cabeçalho x-signature).
 *
 * Segue a especificação oficial de segurança do Mercado Pago:
 * 1. Extração de 'ts' (timestamp) e 'v1' (hash HMAC) do header x-signature.
 * 2. Montagem do template assinado: "id:[data.id];request-id:[x-request-id];ts:[ts];"
 * 3. Cálculo do HMAC-SHA256 com a chave secreta cadastrada na aplicação.
 * 4. Comparação em tempo constante (contra timing attacks).
 * 5. Verificação de tolerância de atraso (replay attack prevention).
 */
@Component
public class MercadoPagoWebhookValidator {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long TOLERANCE_SECONDS = 300; // 5 minutos de tolerância contra replay attack

    private final MercadoPagoProperties properties;

    public MercadoPagoWebhookValidator(MercadoPagoProperties properties) {
        this.properties = properties;
    }

    /**
     * Valida a assinatura do webhook recebido.
     *
     * @param xSignature Conteúdo do header x-signature (ex: "ts=1704908010,v1=618c8534...")
     * @param xRequestId Conteúdo do header x-request-id (UUID)
     * @param dataId     Identificador do recurso notificado (data.id)
     * @throws InvalidWebhookSignatureException Caso a assinatura seja inválida
     */
    public void validarAssinatura(String xSignature, String xRequestId, String dataId) {
        String secret = properties.getWebhookSecret();

        // Se nenhum secret estiver configurado (ambiente local/dev sem secret registrado no painel), apenas avisa
        if (secret == null || secret.isBlank()) {
            log.debug("Validação de x-signature ignorada pois mp.webhook.secret não está configurado.");
            return;
        }

        if (xSignature == null || xSignature.isBlank()) {
            throw new InvalidWebhookSignatureException("Header x-signature obrigatório está ausente.");
        }

        Map<String, String> signatureParts = extrairPartesAssinatura(xSignature);
        String ts = signatureParts.get("ts");
        String v1 = signatureParts.get("v1");

        if (ts == null || v1 == null) {
            throw new InvalidWebhookSignatureException("Header x-signature malformatado: ausência de ts ou v1.");
        }

        // Validação de tolerância de timestamp (proteção contra replay attack)
        try {
            long tsValue = Long.parseLong(ts);
            // Pode vir em segundos ou milissegundos
            long tsSeconds = tsValue > 10_000_000_000L ? tsValue / 1000 : tsValue;
            long nowSeconds = Instant.now().getEpochSecond();
            if (Math.abs(nowSeconds - tsSeconds) > TOLERANCE_SECONDS) {
                log.warn("Timestamp do webhook expirado ou fora da tolerância. Recebido: {}, Atual: {}", tsSeconds, nowSeconds);
                throw new InvalidWebhookSignatureException("Timestamp do webhook expirado fora da tolerância de " + TOLERANCE_SECONDS + "s");
            }
        } catch (NumberFormatException e) {
            throw new InvalidWebhookSignatureException("Timestamp ts em x-signature não é numérico válido.");
        }

        // Construção do template de manifesto segundo documentação do Mercado Pago
        // Formato: id:[data.id];request-id:[x-request-id];ts:[ts];
        StringBuilder manifest = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            manifest.append("id:").append(dataId.toLowerCase().trim()).append(";");
        }
        if (xRequestId != null && !xRequestId.isBlank()) {
            manifest.append("request-id:").append(xRequestId.trim()).append(";");
        }
        manifest.append("ts:").append(ts.trim()).append(";");

        String computedHash = calcularHmacSha256(manifest.toString(), secret);

        // Comparação segura em tempo constante
        if (!MessageDigest.isEqual(computedHash.getBytes(StandardCharsets.UTF_8), v1.toLowerCase().trim().getBytes(StandardCharsets.UTF_8))) {
            log.warn("Assinatura do webhook inválida. Manifest: [{}], Calculado: [{}], Recebido: [{}]", manifest, computedHash, v1);
            throw new InvalidWebhookSignatureException("Assinatura HMAC x-signature não corresponde ao secret configurado.");
        }

        log.debug("Assinatura do webhook validada com sucesso pelo MercadoPagoWebhookValidator.");
    }

    private Map<String, String> extrairPartesAssinatura(String xSignature) {
        Map<String, String> parts = new HashMap<>();
        String[] tokens = xSignature.split(",");
        for (String token : tokens) {
            String[] keyValue = token.split("=", 2);
            if (keyValue.length == 2) {
                parts.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return parts;
    }

    private String calcularHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular HMAC-SHA256 para validação de webhook", e);
        }
    }
}
