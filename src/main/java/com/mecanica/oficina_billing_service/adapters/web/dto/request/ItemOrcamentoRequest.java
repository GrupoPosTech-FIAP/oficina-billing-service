package com.mecanica.oficina_billing_service.adapters.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemOrcamentoRequest {

    @NotBlank(message = "Descrição é obrigatória")
    private String descricao;

    @Positive(message = "Quantidade deve ser maior que zero")
    private int quantidade;

    @Positive(message = "Preço unitário deve ser maior que zero")
    private BigDecimal precoUnitario;

    private String insumoId;
    private String servicoId;
}
