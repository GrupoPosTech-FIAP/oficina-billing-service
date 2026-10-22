package com.mecanica.oficina_billing_service.adapters.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CriarCobrancaRequest {

    @NotBlank(message = "O ID do pedido/ordem de serviço é obrigatório")
    private String pedidoId;

    @NotBlank(message = "A descrição da cobrança é obrigatória")
    private String descricao;

    @NotNull(message = "O valor da cobrança é obrigatório")
    @Positive(message = "O valor deve ser maior que zero")
    private BigDecimal valor;
}
