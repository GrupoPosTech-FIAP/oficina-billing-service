package com.mecanica.oficina_billing_service.adapters.config;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ConsultarCobrancaUseCase;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.CriarCobrancaUseCase;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ProcessarWebhookMercadoPagoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CobrancaConfig {

    @Bean
    public CriarCobrancaUseCase criarCobrancaUseCase(CobrancaGateway cobrancaGateway,
                                                     MercadoPagoGateway mercadoPagoGateway) {
        return new CriarCobrancaUseCase(cobrancaGateway, mercadoPagoGateway);
    }

    @Bean
    public ProcessarWebhookMercadoPagoUseCase processarWebhookMercadoPagoUseCase(CobrancaGateway cobrancaGateway,
                                                                               MercadoPagoGateway mercadoPagoGateway) {
        return new ProcessarWebhookMercadoPagoUseCase(cobrancaGateway, mercadoPagoGateway);
    }

    @Bean
    public ConsultarCobrancaUseCase consultarCobrancaUseCase(CobrancaGateway cobrancaGateway) {
        return new ConsultarCobrancaUseCase(cobrancaGateway);
    }

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return mapper;
    }
}
