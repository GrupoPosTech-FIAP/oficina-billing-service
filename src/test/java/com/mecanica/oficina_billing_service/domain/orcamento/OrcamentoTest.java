package com.mecanica.oficina_billing_service.domain.orcamento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrcamentoTest {

    @Test
    @DisplayName("Deve criar orçamento com status PENDENTE e valor total correto")
    void deveCriarOrcamentoComSucesso() {
        var item1 = new ItemOrcamento("insumo-1", null, "Óleo Motor", 2, new BigDecimal("50.00"));
        var item2 = new ItemOrcamento(null, "servico-1", "Troca de Óleo", 1, new BigDecimal("100.00"));

        var orcamento = Orcamento.criar("os-123", "cli-456", List.of(item1, item2), "Orçamento inicial");

        assertNotNull(orcamento.getId());
        assertEquals("os-123", orcamento.getOrdemServicoId());
        assertEquals("cli-456", orcamento.getClienteId());
        assertEquals(OrcamentoStatus.PENDENTE, orcamento.getStatus());
        assertEquals(new BigDecimal("200.00"), orcamento.getValorTotal());
        assertEquals(2, orcamento.getItens().size());
        assertNotNull(orcamento.getCriadoEm());
        assertNull(orcamento.getRespondidoEm());
    }

    @Test
    @DisplayName("Deve transicionar de PENDENTE para ENVIADO")
    void deveEnviarOrcamento() {
        var item = new ItemOrcamento("insumo-1", null, "Filtro", 1, new BigDecimal("35.00"));
        var orcamento = Orcamento.criar("os-123", "cli-456", List.of(item), "Teste");

        var enviado = orcamento.enviar();

        assertEquals(OrcamentoStatus.ENVIADO, enviado.getStatus());
    }

    @Test
    @DisplayName("Não deve enviar orçamento se não estiver PENDENTE")
    void naoDeveEnviarOrcamentoInvalido() {
        var item = new ItemOrcamento("insumo-1", null, "Filtro", 1, new BigDecimal("35.00"));
        var orcamento = Orcamento.criar("os-123", "cli-456", List.of(item), "Teste").enviar();

        assertThrows(IllegalStateException.class, orcamento::enviar);
    }

    @Test
    @DisplayName("Deve transicionar para AGUARDANDO quando cliente solicitar ajustes")
    void deveAguardarOrcamento() {
        var item = new ItemOrcamento("insumo-1", null, "Filtro", 1, new BigDecimal("35.00"));
        var orcamento = Orcamento.criar("os-123", "cli-456", List.of(item), "Teste").enviar();

        var aguardando = orcamento.aguardar();

        assertEquals(OrcamentoStatus.AGUARDANDO, aguardando.getStatus());
    }

    @Test
    @DisplayName("Deve aprovar orçamento enviado ou em aguardo")
    void deveAprovarOrcamento() {
        var item = new ItemOrcamento("insumo-1", null, "Filtro", 1, new BigDecimal("35.00"));
        var orcamento = Orcamento.criar("os-123", "cli-456", List.of(item), "Teste").enviar();

        var aprovado = orcamento.aprovar();

        assertEquals(OrcamentoStatus.APROVADO, aprovado.getStatus());
        assertNotNull(aprovado.getRespondidoEm());
    }

    @Test
    @DisplayName("Deve negar orçamento")
    void deveNegarOrcamento() {
        var item = new ItemOrcamento("insumo-1", null, "Filtro", 1, new BigDecimal("35.00"));
        var orcamento = Orcamento.criar("os-123", "cli-456", List.of(item), "Teste").enviar();

        var negado = orcamento.negar();

        assertEquals(OrcamentoStatus.NEGADO, negado.getStatus());
        assertNotNull(negado.getRespondidoEm());
    }

    @Test
    @DisplayName("Deve atualizar itens quando em estado AGUARDANDO")
    void deveAtualizarItens() {
        var item1 = new ItemOrcamento("insumo-1", null, "Filtro", 1, new BigDecimal("35.00"));
        var orcamento = Orcamento.criar("os-123", "cli-456", List.of(item1), "Teste").enviar().aguardar();

        var novoItem = new ItemOrcamento("insumo-2", null, "Filtro Premium", 1, new BigDecimal("60.00"));
        var atualizado = orcamento.atualizar(List.of(novoItem), "Ajustado com desconto");

        assertEquals(new BigDecimal("60.00"), atualizado.getValorTotal());
        assertEquals("Ajustado com desconto", atualizado.getObservacoes());
    }
}
