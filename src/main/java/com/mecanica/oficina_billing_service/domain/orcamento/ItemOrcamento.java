package com.mecanica.oficina_billing_service.domain.orcamento;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class ItemOrcamento {

    private final String id;
    private final String insumoId;
    private final String servicoId;
    private final String descricao;
    private final int quantidade;
    private final BigDecimal precoUnitario;

    public ItemOrcamento(String insumoId, String servicoId, String descricao, int quantidade, BigDecimal precoUnitario) {
        this(UUID.randomUUID().toString(), insumoId, servicoId, descricao, quantidade, precoUnitario);
    }

    public ItemOrcamento(String id, String insumoId, String servicoId, String descricao, int quantidade, BigDecimal precoUnitario) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.insumoId = insumoId;
        this.servicoId = servicoId;
        this.descricao = Objects.requireNonNull(descricao, "Descrição é obrigatória");
        Objects.requireNonNull(precoUnitario, "Preço unitário é obrigatório");
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        if (precoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço unitário deve ser maior que zero");
        }
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getValorTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public String getId() {
        return id;
    }

    public String getInsumoId() {
        return insumoId;
    }

    public String getServicoId() {
        return servicoId;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}
