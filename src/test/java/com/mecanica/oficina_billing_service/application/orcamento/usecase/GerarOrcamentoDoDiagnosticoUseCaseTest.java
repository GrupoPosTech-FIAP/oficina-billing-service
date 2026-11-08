package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.input.GerarOrcamentoInput;
import com.mecanica.oficina_billing_service.application.orcamento.input.ItemServicoInput;
import com.mecanica.oficina_billing_service.domain.orcamento.ItemOrcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GerarOrcamentoDoDiagnosticoUseCaseTest {

    private static final GerarOrcamentoInput INPUT = new GerarOrcamentoInput("os-1", "cli-1", List.of(),
            List.of(new ItemServicoInput("srv-1", 1, new BigDecimal("80.00"), "Troca de óleo")), null);

    private OrcamentoGateway orcamentoGateway;
    private GerarOrcamentoUseCase gerar;
    private EnviarOrcamentoUseCase enviar;
    private GerarOrcamentoDoDiagnosticoUseCase useCase;

    @BeforeEach
    void setUp() {
        orcamentoGateway = mock(OrcamentoGateway.class);
        gerar = mock(GerarOrcamentoUseCase.class);
        enviar = mock(EnviarOrcamentoUseCase.class);
        useCase = new GerarOrcamentoDoDiagnosticoUseCase(orcamentoGateway, gerar, enviar);
    }

    private static Orcamento orcamento(OrcamentoStatus status) {
        return Orcamento.reconstituir("orc-1", "os-1", "cli-1",
                List.of(new ItemOrcamento(null, "srv-1", "Troca de óleo", 1, new BigDecimal("80.00"))),
                status, null, LocalDateTime.now(), null);
    }

    @Test
    void geraEEnviaOrcamento() {
        when(orcamentoGateway.buscarPorOrdemServicoId("os-1")).thenReturn(Optional.empty());
        when(gerar.executar(INPUT)).thenReturn(orcamento(OrcamentoStatus.PENDENTE));
        Orcamento enviado = orcamento(OrcamentoStatus.ENVIADO);
        when(enviar.executar("orc-1")).thenReturn(enviado);

        assertThat(useCase.executar(INPUT)).contains(enviado);
    }

    @Test
    void geraNovoQuandoOAnteriorFoiNegadoOuExpirou() {
        when(orcamentoGateway.buscarPorOrdemServicoId("os-1")).thenReturn(Optional.of(orcamento(OrcamentoStatus.EXPIRADO)));
        when(gerar.executar(INPUT)).thenReturn(orcamento(OrcamentoStatus.PENDENTE));
        when(enviar.executar("orc-1")).thenReturn(orcamento(OrcamentoStatus.ENVIADO));

        assertThat(useCase.executar(INPUT)).isPresent();

        verify(enviar).executar("orc-1");
    }

    @Test
    void orcamentoEmAndamentoNaoGeraOutro() {
        when(orcamentoGateway.buscarPorOrdemServicoId("os-1")).thenReturn(Optional.of(orcamento(OrcamentoStatus.ENVIADO)));

        assertThat(useCase.executar(INPUT)).isEmpty();

        verifyNoInteractions(gerar, enviar);
    }
}
