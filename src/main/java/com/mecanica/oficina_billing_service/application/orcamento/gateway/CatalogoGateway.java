package com.mecanica.oficina_billing_service.application.orcamento.gateway;

import java.math.BigDecimal;
import java.util.Optional;

public interface CatalogoGateway {
    record ItemCatalogo(String id, String nome, BigDecimal precoUnitario) {}

    Optional<ItemCatalogo> buscarInsumo(String insumoId);
    Optional<ItemCatalogo> buscarServico(String servicoId);
}
