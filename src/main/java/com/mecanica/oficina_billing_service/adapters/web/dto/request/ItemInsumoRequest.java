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
public class ItemInsumoRequest {

    @NotBlank(message = "O ID do insumo é obrigatório")
    private String insumoId;

    @Positive(message = "A quantidade de insumo deve ser maior que zero")
    private int quantidade;

    private BigDecimal precoUnitario;
    private String descricao;
}
