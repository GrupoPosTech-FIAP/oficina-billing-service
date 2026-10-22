package com.mecanica.oficina_billing_service.application.orcamento;

import com.mecanica.oficina_billing_service.application.orcamento.gateway.CatalogoGateway;
import com.mecanica.oficina_billing_service.application.orcamento.input.AtualizarOrcamentoInput;
import com.mecanica.oficina_billing_service.application.orcamento.input.GerarOrcamentoInput;
import com.mecanica.oficina_billing_service.application.orcamento.input.ItemInsumoInput;
import com.mecanica.oficina_billing_service.application.orcamento.input.ItemOrcamentoInput;
import com.mecanica.oficina_billing_service.application.orcamento.input.ItemServicoInput;
import com.mecanica.oficina_billing_service.domain.orcamento.ItemOrcamento;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MontadorItensOrcamento {

    private final CatalogoGateway catalogoGateway;

    public MontadorItensOrcamento() {
        this(null);
    }

    public MontadorItensOrcamento(CatalogoGateway catalogoGateway) {
        this.catalogoGateway = catalogoGateway;
    }

    public List<ItemOrcamento> montar(GerarOrcamentoInput input) {
        return montar(input.itens(), input.insumos(), input.servicos());
    }

    public List<ItemOrcamento> montar(AtualizarOrcamentoInput input) {
        return montar(input.itens(), input.insumos(), input.servicos());
    }

    public List<ItemOrcamento> montar(List<ItemOrcamentoInput> itensInput,
                                      List<ItemInsumoInput> insumosInput,
                                      List<ItemServicoInput> servicosInput) {
        List<ItemOrcamento> itens = new ArrayList<>();

        if (itensInput != null) {
            for (var item : itensInput) {
                itens.add(new ItemOrcamento(
                        item.insumoId(),
                        item.servicoId(),
                        item.descricao(),
                        item.quantidade(),
                        item.precoUnitario()
                ));
            }
        }

        if (insumosInput != null) {
            for (var i : insumosInput) {
                String descricao = i.descricao();
                BigDecimal preco = i.precoUnitario();

                if ((descricao == null || preco == null) && catalogoGateway != null) {
                    var itemCat = catalogoGateway.buscarInsumo(i.insumoId())
                            .orElseThrow(() -> new IllegalArgumentException("Insumo não encontrado no catálogo: " + i.insumoId()));
                    if (descricao == null) descricao = itemCat.nome();
                    if (preco == null) preco = itemCat.precoUnitario();
                }

                if (descricao == null || preco == null) {
                    throw new IllegalArgumentException("Descrição e preço unitário devem ser fornecidos para o insumo: " + i.insumoId());
                }

                itens.add(new ItemOrcamento(i.insumoId(), null, descricao, i.quantidade(), preco));
            }
        }

        if (servicosInput != null) {
            for (var s : servicosInput) {
                String descricao = s.descricao();
                BigDecimal preco = s.precoUnitario();

                if ((descricao == null || preco == null) && catalogoGateway != null) {
                    var servCat = catalogoGateway.buscarServico(s.servicoId())
                            .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado no catálogo: " + s.servicoId()));
                    if (descricao == null) descricao = servCat.nome();
                    if (preco == null) preco = servCat.precoUnitario();
                }

                if (descricao == null || preco == null) {
                    throw new IllegalArgumentException("Descrição e preço unitário devem ser fornecidos para o serviço: " + s.servicoId());
                }

                itens.add(new ItemOrcamento(null, s.servicoId(), descricao, s.quantidade(), preco));
            }
        }

        if (itens.isEmpty()) {
            throw new IllegalArgumentException("Orçamento deve ter ao menos um item");
        }

        return itens;
    }
}
