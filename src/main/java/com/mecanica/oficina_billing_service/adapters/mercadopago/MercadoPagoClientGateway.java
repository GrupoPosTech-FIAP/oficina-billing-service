package com.mecanica.oficina_billing_service.adapters.mercadopago;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class MercadoPagoClientGateway implements MercadoPagoGateway {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClientGateway.class);

    private final String notificationUrl;

    public MercadoPagoClientGateway(
            @Value("${mp.access.token:}") String accessToken,
            @Value("${mp.webhook.public.url:}") String notificationUrl) {
        String token = (accessToken != null && !accessToken.isBlank())
                ? accessToken
                : System.getenv("MP_ACCESS_TOKEN");

        if (token != null && !token.isBlank()) {
            MercadoPagoConfig.setAccessToken(token);
            log.info("Mercado Pago SDK configurado com Access Token fornecido.");
        } else {
            log.warn("Nenhum MP_ACCESS_TOKEN configurado. Operações no Mercado Pago utilizarão modo simulado/mock se necessário.");
        }

        this.notificationUrl = (notificationUrl != null && !notificationUrl.isBlank())
                ? notificationUrl
                : System.getenv("MP_WEBHOOK_PUBLIC_URL");
    }

    @Override
    public PreferenciaPagamento criarPreferencia(String referencia, String descricao, BigDecimal valor) {
        try {
            var item = PreferenceItemRequest.builder()
                    .title(descricao != null && !descricao.isBlank() ? descricao : "Serviço Mecânica")
                    .quantity(1)
                    .currencyId("BRL")
                    .unitPrice(valor)
                    .build();

            var prefReqBuilder = PreferenceRequest.builder()
                    .items(List.of(item))
                    .externalReference(referencia);

            if (notificationUrl != null && !notificationUrl.isBlank()) {
                prefReqBuilder.notificationUrl(notificationUrl);
            }

            Preference pref = new PreferenceClient().create(prefReqBuilder.build());
            return new PreferenciaPagamento(pref.getId(), pref.getInitPoint());
        } catch (Exception e) {
            log.error("Erro ao comunicar com Mercado Pago: {}", e.getMessage());
            throw new RuntimeException("Falha ao criar preferência de pagamento no Mercado Pago: " + e.getMessage(), e);
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
                    payment.getExternalReference()
            ));
        } catch (Exception e) {
            log.error("Erro ao consultar pagamento {} no Mercado Pago: {}", paymentId, e.getMessage());
            return Optional.empty();
        }
    }
}
