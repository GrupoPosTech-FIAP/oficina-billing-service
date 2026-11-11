package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ExpirarCobrancasUseCaseTest {

    private static final Instant AGORA = Instant.parse("2026-11-08T12:00:00Z");

    private static Cobranca pendente(String os, OffsetDateTime criadaEm) {
        return Cobranca.reconstituir(UUID.randomUUID(), os, BigDecimal.TEN, null, "ref-" + os, "pref-" + os, "url",
                CobrancaStatus.PENDENTE, criadaEm, criadaEm);
    }

    @Test
    void cancelaPendentesForaDoPrazoEPublicaPagamentoExpirado() {
        CobrancaGateway gateway = mock(CobrancaGateway.class);
        PublicadorEventosGateway publicador = mock(PublicadorEventosGateway.class);
        OffsetDateTime agora = AGORA.atOffset(ZoneOffset.UTC);
        Cobranca vencida = pendente("os-1", agora.minusHours(25));
        when(gateway.listarPorStatus(CobrancaStatus.PENDENTE)).thenReturn(List.of(vencida, pendente("os-2", agora.minusHours(1))));
        when(gateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        int expiradas = new ExpirarCobrancasUseCase(gateway, publicador, Duration.ofHours(24),
                Clock.fixed(AGORA, ZoneOffset.UTC)).executar();

        assertThat(expiradas).isEqualTo(1);
        verify(gateway).atualizar(argThat(c -> c.getPedidoId().equals("os-1") && c.getStatus() == CobrancaStatus.CANCELADO));
        verify(publicador).publicar("PagamentoExpirado", "os-1",
                Map.of("ordemServicoId", "os-1", "cobrancaId", vencida.getId().toString()));
        verifyNoMoreInteractions(publicador);
    }
}
