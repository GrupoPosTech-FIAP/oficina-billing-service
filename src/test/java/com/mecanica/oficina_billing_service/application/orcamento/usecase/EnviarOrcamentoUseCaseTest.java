package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.orcamento.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.output.NotificacaoCliente;
import com.mecanica.oficina_billing_service.domain.orcamento.ItemOrcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnviarOrcamentoUseCaseTest {

    @Mock
    private OrcamentoGateway orcamentoGateway;

    @Mock
    private NotificadorClienteGateway notificadorClienteGateway;

    private EnviarOrcamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EnviarOrcamentoUseCase(orcamentoGateway, notificadorClienteGateway);
    }

    @Test
    @DisplayName("Deve enviar orçamento para aprovação e disparar notificação")
    void deveEnviarOrcamentoENotificar() {
        var item = new ItemOrcamento("insumo-1", null, "Pneu", 4, new BigDecimal("350.00"));
        var orcamento = Orcamento.criar("os-1", "cli-1", List.of(item), "Troca de pneus");

        when(orcamentoGateway.buscarPorId(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(orcamentoGateway.atualizar(any(Orcamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = useCase.executar(orcamento.getId());

        assertNotNull(resultado);
        assertEquals(OrcamentoStatus.ENVIADO, resultado.getStatus());

        verify(orcamentoGateway).atualizar(any(Orcamento.class));
        verify(notificadorClienteGateway).notificar(any(NotificacaoCliente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção se orçamento não existir")
    void deveLancarExcecaoSeNaoEncontrado() {
        when(orcamentoGateway.buscarPorId("id-inexistente")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.executar("id-inexistente"));
        verify(notificadorClienteGateway, never()).notificar(any());
    }
}
