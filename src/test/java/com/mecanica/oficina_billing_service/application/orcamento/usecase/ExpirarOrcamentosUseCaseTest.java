package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.domain.orcamento.ItemOrcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ExpirarOrcamentosUseCaseTest {

    private static final Instant AGORA = Instant.parse("2026-11-08T12:00:00Z");
    private static final LocalDateTime AGORA_LOCAL = LocalDateTime.ofInstant(AGORA, ZoneId.systemDefault());

    private static Orcamento orcamento(String id, LocalDateTime criadoEm) {
        return Orcamento.reconstituir(id, "os-" + id, "cli-1",
                List.of(new ItemOrcamento(null, "srv-1", "Revisão", 1, BigDecimal.TEN)),
                OrcamentoStatus.ENVIADO, null, criadoEm, null);
    }

    @Test
    void expiraSoOsQuePassaramDoPrazoEPublicaOrcamentoExpirado() {
        OrcamentoGateway gateway = mock(OrcamentoGateway.class);
        PublicadorEventosGateway publicador = mock(PublicadorEventosGateway.class);
        when(gateway.listarPorStatus(List.of(OrcamentoStatus.ENVIADO, OrcamentoStatus.AGUARDANDO))).thenReturn(List.of(
                orcamento("1", AGORA_LOCAL.minusHours(3)),
                orcamento("2", AGORA_LOCAL.minusMinutes(30))));
        when(gateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        int expirados = new ExpirarOrcamentosUseCase(gateway, publicador, Duration.ofHours(2),
                Clock.fixed(AGORA, ZoneId.of("UTC"))).executar();

        assertThat(expirados).isEqualTo(1);
        verify(gateway).atualizar(argThat(o -> o.getId().equals("1") && o.getStatus() == OrcamentoStatus.EXPIRADO));
        verify(publicador).publicar("OrcamentoExpirado", "os-1", Map.of("ordemServicoId", "os-1", "orcamentoId", "1"));
        verifyNoMoreInteractions(publicador);
    }
}
