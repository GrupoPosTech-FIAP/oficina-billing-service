package com.mecanica.oficina_billing_service.adapters.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GerarOrcamentoRequest {

    @NotBlank(message = "O ID da ordem de serviço é obrigatório")
    private String ordemServicoId;

    private String clienteId;

    private List<@Valid ItemOrcamentoRequest> itens;

    private List<@Valid ItemInsumoRequest> insumos;

    private List<@Valid ItemServicoRequest> servicos;

    private String observacoes;
}
