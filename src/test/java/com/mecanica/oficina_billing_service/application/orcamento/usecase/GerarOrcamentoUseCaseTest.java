package com.mecanica.oficina_billing_service.application.orcamento.usecase;

import com.mecanica.oficina_billing_service.application.orcamento.MontadorItensOrcamento;
import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.input.GerarOrcamentoInput;
import com.mecanica.oficina_billing_service.application.orcamento.input.ItemOrcamentoInput;
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
class GerarOrcamentoUseCaseTest {

    @Mock
    private OrcamentoGateway orcamentoGateway;

    private MontadorItensOrcamento montadorItensOrcamento;
    private GerarOrcamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        montadorItensOrcamento = new MontadorItensOrcamento();
        useCase = new GerarOrcamentoUseCase(orcamentoGateway, montadorItensOrcamento);
    }

    @Test
    @DisplayName("Deve gerar orçamento com sucesso")
    void deveGerarOrcamentoComSucesso() {
        var itemInput = new ItemOrcamentoInput("Troca de Óleo", 1, new BigDecimal("150.00"));
        var input = new GerarOrcamentoInput("os-1", "cli-1", List.of(itemInput), null, null, "Observação de teste");

        when(orcamentoGateway.buscarPorOrdemServicoId("os-1")).thenReturn(Optional.empty());
        when(orcamentoGateway.salvar(any(Orcamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = useCase.executar(input);

        assertNotNull(resultado);
        assertEquals("os-1", resultado.getOrdemServicoId());
        assertEquals("cli-1", resultado.getClienteId());
        assertEquals(OrcamentoStatus.PENDENTE, resultado.getStatus());
        assertEquals(new BigDecimal("150.00"), resultado.getValorTotal());

        verify(orcamentoGateway).salvar(any(Orcamento.class));
    }

    @Test
    @DisplayName("Não deve gerar orçamento se já existir um orçamento ativo para a OS")
    void naoDeveGerarOrcamentoSeJaExisteAtivo() {
        var itemInput = new ItemOrcamentoInput("Troca de Óleo", 1, new BigDecimal("150.00"));
        var input = new GerarOrcamentoInput("os-1", "cli-1", List.of(itemInput), null, null, "Teste");

        var existente = Orcamento.criar("os-1", "cli-1", montadorItensOrcamento.montar(input), "Existente");
        when(orcamentoGateway.buscarPorOrdemServicoId("os-1")).thenReturn(Optional.of(existente));

        assertThrows(IllegalStateException.class, () -> useCase.executar(input));
        verify(orcamentoGateway, never()).salvar(any(Orcamento.class));
    }
}
