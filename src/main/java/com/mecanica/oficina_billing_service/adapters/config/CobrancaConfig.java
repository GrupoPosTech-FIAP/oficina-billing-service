package com.mecanica.oficina_billing_service.adapters.config;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.CobrarOrdemServicoUseCase;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ConsultarCobrancaUseCase;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.CriarCobrancaUseCase;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.EstornarPagamentoOrdemServicoUseCase;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ExpirarCobrancasUseCase;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ProcessarWebhookMercadoPagoUseCase;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
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
                                                                               MercadoPagoGateway mercadoPagoGateway,
                                                                               PublicadorEventosGateway publicador) {
        return new ProcessarWebhookMercadoPagoUseCase(cobrancaGateway, mercadoPagoGateway, publicador);
    }

    @Bean
    public CobrarOrdemServicoUseCase cobrarOrdemServicoUseCase(OrcamentoGateway orcamentoGateway,
                                                             CobrancaGateway cobrancaGateway,
                                                             CriarCobrancaUseCase criarCobrancaUseCase,
                                                             PublicadorEventosGateway publicador) {
        return new CobrarOrdemServicoUseCase(orcamentoGateway, cobrancaGateway, criarCobrancaUseCase, publicador);
    }

    @Bean
    public EstornarPagamentoOrdemServicoUseCase estornarPagamentoOrdemServicoUseCase(CobrancaGateway cobrancaGateway,
                                                                                   MercadoPagoGateway mercadoPagoGateway,
                                                                                   PublicadorEventosGateway publicador) {
        return new EstornarPagamentoOrdemServicoUseCase(cobrancaGateway, mercadoPagoGateway, publicador);
    }

    @Bean
    public ExpirarCobrancasUseCase expirarCobrancasUseCase(CobrancaGateway cobrancaGateway,
                                                         PublicadorEventosGateway publicador,
                                                         @Value("${saga.cobranca.expiracao:PT24H}") Duration prazo,
                                                         Clock clock) {
        return new ExpirarCobrancasUseCase(cobrancaGateway, publicador, prazo, clock);
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
