package com.mecanica.oficina_billing_service.adapters.messaging;

import com.mecanica.oficina_billing_service.adapters.persistence.OutboxEventoSpringDataRepository;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ExpirarCobrancasUseCase;
import com.mecanica.oficina_billing_service.application.orcamento.usecase.ExpirarOrcamentosUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.support.TransactionOperations;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MensageriaConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MensageriaConfig.class)
            .withBean(OutboxEventoSpringDataRepository.class, () -> mock(OutboxEventoSpringDataRepository.class))
            .withBean(ProcessadorEventos.class, () -> mock(ProcessadorEventos.class))
            .withBean(JsonMapper.class, () -> MensageriaFixture.JSON)
            .withBean(ExpirarOrcamentosUseCase.class, () -> mock(ExpirarOrcamentosUseCase.class))
            .withBean(ExpirarCobrancasUseCase.class, () -> mock(ExpirarCobrancasUseCase.class))
            .withBean(TransactionOperations.class, TransactionOperations::withoutTransaction);

    @Test
    void desligadaPorPadraoNaoCriaClientesNemAgendadores() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(MensageriaProperties.class);
            assertThat(ctx).doesNotHaveBean(SnsClient.class);
            assertThat(ctx).doesNotHaveBean(SqsClient.class);
            assertThat(ctx).doesNotHaveBean(OutboxRelay.class);
            assertThat(ctx).doesNotHaveBean(SqsConsumidorEventos.class);
            assertThat(ctx).doesNotHaveBean(ExpiracaoSagaAgendador.class);
            assertThat(ctx.getBean(MensageriaProperties.class).produtor()).isEqualTo("oficina-billing-service");
        });
    }

    @Test
    void habilitadaCriaClientesApontandoParaOEndpointConfigurado() {
        runner.withPropertyValues(
                        "mensageria.habilitada=true",
                        "mensageria.endpoint=http://localhost:4566",
                        "mensageria.topico-arn=arn:aws:sns:us-east-1:000000000000:oficina-billing-events",
                        "mensageria.fila-url=http://localhost:4566/000000000000/oficina-billing-queue")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(SnsClient.class);
                    assertThat(ctx).hasSingleBean(SqsClient.class);
                    assertThat(ctx).hasSingleBean(OutboxRelay.class);
                    assertThat(ctx).hasSingleBean(SqsConsumidorEventos.class);
                    assertThat(ctx).hasSingleBean(ExpiracaoSagaAgendador.class);
                    assertThat(ctx.getBean(MensageriaProperties.class).topicoArn()).endsWith("oficina-billing-events");
                });
    }

    @Test
    void habilitadaSemEndpointUsaAAws() {
        runner.withPropertyValues("mensageria.habilitada=true", "mensageria.regiao=sa-east-1")
                .run(ctx -> assertThat(ctx).hasSingleBean(SnsClient.class));
    }

    @Test
    void valoresPadraoDasPropriedades() {
        MensageriaProperties p = new MensageriaProperties(false, " ", null, null, "", null, null, null, null);

        assertThat(p.produtor()).isEqualTo("oficina-billing-service");
        assertThat(p.regiao()).isEqualTo("us-east-1");
        assertThat(p.loteOutbox()).isEqualTo(50);
        assertThat(p.maxMensagens()).isEqualTo(10);
        assertThat(p.esperaSegundos()).isEqualTo(10);
    }
}
