package com.mecanica.oficina_billing_service.adapters.config;

import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.application.orcamento.MontadorItensOrcamento;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.CatalogoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.usecase.*;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrcamentoConfig {

    @Bean
    public MontadorItensOrcamento montadorItensOrcamento(@Autowired(required = false) CatalogoGateway catalogoGateway) {
        return new MontadorItensOrcamento(catalogoGateway);
    }

    @Bean
    public GerarOrcamentoUseCase gerarOrcamentoUseCase(OrcamentoGateway orcamentoGateway,
                                                     MontadorItensOrcamento montadorItensOrcamento) {
        return new GerarOrcamentoUseCase(orcamentoGateway, montadorItensOrcamento);
    }

    @Bean
    public EnviarOrcamentoUseCase enviarOrcamentoUseCase(OrcamentoGateway orcamentoGateway,
                                                     PublicadorEventosGateway publicador,
                                                       NotificadorClienteGateway notificadorClienteGateway) {
        return new EnviarOrcamentoUseCase(orcamentoGateway, notificadorClienteGateway, publicador);
    }

    @Bean
    public AtualizarOrcamentoUseCase atualizarOrcamentoUseCase(OrcamentoGateway orcamentoGateway,
                                                             MontadorItensOrcamento montadorItensOrcamento) {
        return new AtualizarOrcamentoUseCase(orcamentoGateway, montadorItensOrcamento);
    }

    @Bean
    public AguardarOrcamentoUseCase aguardarOrcamentoUseCase(OrcamentoGateway orcamentoGateway) {
        return new AguardarOrcamentoUseCase(orcamentoGateway);
    }

    @Bean
    public AprovarOrcamentoUseCase aprovarOrcamentoUseCase(OrcamentoGateway orcamentoGateway,
                                                     PublicadorEventosGateway publicador,
                                                         NotificadorClienteGateway notificadorClienteGateway) {
        return new AprovarOrcamentoUseCase(orcamentoGateway, notificadorClienteGateway, publicador);
    }

    @Bean
    public NegarOrcamentoUseCase negarOrcamentoUseCase(OrcamentoGateway orcamentoGateway,
                                                     PublicadorEventosGateway publicador,
                                                     NotificadorClienteGateway notificadorClienteGateway) {
        return new NegarOrcamentoUseCase(orcamentoGateway, notificadorClienteGateway, publicador);
    }

    @Bean
    public GerarOrcamentoDoDiagnosticoUseCase gerarOrcamentoDoDiagnosticoUseCase(OrcamentoGateway orcamentoGateway,
                                                                             GerarOrcamentoUseCase gerarOrcamentoUseCase,
                                                                             EnviarOrcamentoUseCase enviarOrcamentoUseCase) {
        return new GerarOrcamentoDoDiagnosticoUseCase(orcamentoGateway, gerarOrcamentoUseCase, enviarOrcamentoUseCase);
    }

    @Bean
    public ExpirarOrcamentosUseCase expirarOrcamentosUseCase(OrcamentoGateway orcamentoGateway,
                                                           PublicadorEventosGateway publicador,
                                                           @Value("${saga.orcamento.expiracao:PT72H}") Duration prazo,
                                                           Clock clock) {
        return new ExpirarOrcamentosUseCase(orcamentoGateway, publicador, prazo, clock);
    }

    @Bean
    public ConsultarOrcamentoUseCase consultarOrcamentoUseCase(OrcamentoGateway orcamentoGateway) {
        return new ConsultarOrcamentoUseCase(orcamentoGateway);
    }
}
