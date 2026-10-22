package com.mecanica.oficina_billing_service.adapters.config;

import com.mecanica.oficina_billing_service.application.orcamento.MontadorItensOrcamento;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.CatalogoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.usecase.*;
import org.springframework.beans.factory.annotation.Autowired;
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
                                                       NotificadorClienteGateway notificadorClienteGateway) {
        return new EnviarOrcamentoUseCase(orcamentoGateway, notificadorClienteGateway);
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
                                                         NotificadorClienteGateway notificadorClienteGateway) {
        return new AprovarOrcamentoUseCase(orcamentoGateway, notificadorClienteGateway);
    }

    @Bean
    public NegarOrcamentoUseCase negarOrcamentoUseCase(OrcamentoGateway orcamentoGateway,
                                                     NotificadorClienteGateway notificadorClienteGateway) {
        return new NegarOrcamentoUseCase(orcamentoGateway, notificadorClienteGateway);
    }

    @Bean
    public ConsultarOrcamentoUseCase consultarOrcamentoUseCase(OrcamentoGateway orcamentoGateway) {
        return new ConsultarOrcamentoUseCase(orcamentoGateway);
    }
}
