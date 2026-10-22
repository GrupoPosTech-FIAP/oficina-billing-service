package com.mecanica.oficina_billing_service.adapters.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemOrcamentoResponse {
    private String id;
    private String insumoId;
    private String servicoId;
    private String descricao;
    private int quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal valorTotal;
}
