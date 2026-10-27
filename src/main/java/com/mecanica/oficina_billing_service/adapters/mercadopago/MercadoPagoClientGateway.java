package com.mecanica.oficina_billing_service.adapters.mercadopago;

import com.mecanica.oficina_billing_service.adapters.config.MercadoPagoProperties;
import com.mecanica.oficina_billing_service.application.cobranca.exception.MercadoPagoIntegrationException;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de comunicação com as APIs e SDK oficial do Mercado Pago para Checkout Pro.
 */
@Component
public class MercadoPagoClientGateway implements MercadoPagoGateway {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClientGateway.class);

    private final MercadoPagoProperties properties;

    public MercadoPagoClientGateway(MercadoPagoProperties properties) {
        this.properties = properties;

        String token = properties.getAccessToken();
        if (token == null || token.isBlank()) {
            token = System.getenv("MP_ACCESS_TOKEN");
        }

        if (token != null && !token.isBlank()) {
            MercadoPagoConfig.setAccessToken(token.trim());
            log.info("Mercado Pago SDK Java inicializado com sucesso para Checkout Pro.");
        } else {
            log.warn("Nenhum MP_ACCESS_TOKEN configurado. Chamadas reais ao Mercado Pago falharão até que o token seja definido via .env ou variável de ambiente.");
        }
    }

    @Override
    public PreferenciaPagamento criarPreferencia(String referencia, String descricao, BigDecimal valor) {
        return criarPreferencia(referencia, descricao, valor, null, null, null, null);
    }

    @Override
    public PreferenciaPagamento criarPreferencia(String referencia, String descricao, BigDecimal valor,
                                                String payerEmail, String payerName, String payerCpf) {
        return criarPreferencia(referencia, descricao, valor, payerEmail, payerName, payerCpf, null);
    }

    @Override
    public PreferenciaPagamento criarPreferencia(String referencia, String descricao, BigDecimal valor,
                                                String payerEmail, String payerName, String payerCpf, String returnUrl) {
        if (MercadoPagoConfig.getAccessToken() == null || MercadoPagoConfig.getAccessToken().isBlank()) {
            throw new MercadoPagoIntegrationException("Credencial do Mercado Pago não configurada. Defina a variável de ambiente MP_ACCESS_TOKEN ou configure o arquivo .env.");
        }
        try {
            // 1. Configuração do item cobrado
            var item = PreferenceItemRequest.builder()
                    .id(referencia)
                    .title(descricao != null && !descricao.isBlank() ? descricao : "Serviço Mecânica")
                    .quantity(1)
                    .currencyId("BRL")
                    .unitPrice(valor)
                    .build();

            // 2. Configuração das URLs de retorno do Checkout Pro (sucesso, falha, pendente)
            String successUrl = (returnUrl != null && !returnUrl.isBlank()) ? returnUrl : properties.getBackUrlSuccess();
            String failureUrl = (returnUrl != null && !returnUrl.isBlank()) ? returnUrl : properties.getBackUrlFailure();
            String pendingUrl = (returnUrl != null && !returnUrl.isBlank()) ? returnUrl : properties.getBackUrlPending();

            var backUrls = PreferenceBackUrlsRequest.builder()
                    .success(successUrl)
                    .failure(failureUrl)
                    .pending(pendingUrl)
                    .build();

            var prefReqBuilder = PreferenceRequest.builder()
                    .items(List.of(item))
                    .externalReference(referencia)
                    .backUrls(backUrls)
                    .autoReturn(properties.getAutoReturn())
                    .statementDescriptor(properties.getStatementDescriptor());

            // 3. Webhook notification URL
            String notifUrl = properties.getWebhookPublicUrl();
            if (notifUrl != null && !notifUrl.isBlank()) {
                prefReqBuilder.notificationUrl(notifUrl);
            }

            // 4. Dados do pagador (se informados) para pré-preenchimento e prevenção de fraude
            if (payerEmail != null && !payerEmail.isBlank()) {
                var payerBuilder = PreferencePayerRequest.builder()
                        .email(payerEmail.trim());

                if (payerName != null && !payerName.isBlank()) {
                    payerBuilder.name(payerName.trim());
                }

                if (payerCpf != null && !payerCpf.isBlank()) {
                    payerBuilder.identification(IdentificationRequest.builder()
                            .type("CPF")
                            .number(payerCpf.replaceAll("\\D", ""))
                            .build());
                }

                prefReqBuilder.payer(payerBuilder.build());
            }

            // 5. Configuração de idempotência recomendada pelo Mercado Pago
            Map<String, String> customHeaders = new HashMap<>();
            customHeaders.put("X-Idempotency-Key", UUID.randomUUID().toString());
            var requestOptions = MPRequestOptions.builder()
                    .customHeaders(customHeaders)
                    .build();

            log.info("Criando preferência de Checkout Pro no Mercado Pago para ref: {}", referencia);
            Preference pref = new PreferenceClient().create(prefReqBuilder.build(), requestOptions);

            log.info("Preferência criada com sucesso. ID: {}, InitPoint: {}", pref.getId(), pref.getInitPoint());
            return new PreferenciaPagamento(pref.getId(), pref.getInitPoint(), pref.getSandboxInitPoint());

        } catch (MPApiException apiException) {
            log.error("Erro na API Mercado Pago (HTTP {}): {}", apiException.getStatusCode(), apiException.getApiResponse().getContent());
            throw new MercadoPagoIntegrationException("Erro ao criar preferência no Mercado Pago: " + apiException.getApiResponse().getContent(), apiException);
        } catch (MPException mpException) {
            log.error("Erro no SDK Mercado Pago: {}", mpException.getMessage());
            throw new MercadoPagoIntegrationException("Falha de conexão com Mercado Pago: " + mpException.getMessage(), mpException);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar preferência: {}", e.getMessage(), e);
            throw new MercadoPagoIntegrationException("Erro inesperado ao criar preferência de pagamento: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<DetalhePagamento> consultarPagamento(Long paymentId) {
        try {
            Payment payment = new PaymentClient().get(paymentId);
            if (payment == null) {
                return Optional.empty();
            }

            return Optional.of(new DetalhePagamento(
                    payment.getId(),
                    payment.getStatus(),
                    payment.getStatusDetail(),
                    payment.getExternalReference(),
                    payment.getPaymentMethodId(),
                    payment.getTransactionAmount()
            ));
        } catch (Exception e) {
            log.error("Erro ao consultar pagamento {} no Mercado Pago: {}", paymentId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getPublicKey() {
        return properties.getPublicKey();
    }

    @Override
    public boolean isSandboxMode() {
        return properties.isSandboxMode();
    }
}
