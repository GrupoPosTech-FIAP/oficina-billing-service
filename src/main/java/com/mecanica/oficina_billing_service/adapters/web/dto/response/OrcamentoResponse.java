package com.mecanica.oficina_billing_service.adapters.web.dto.response;

import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrcamentoResponse {
    private String id;
    private String ordemServicoId;
    private String clienteId;
    private BigDecimal valorTotal;
    private OrcamentoStatus status;
    private String observacoes;
    private LocalDateTime criadoEm;
    private LocalDateTime respondidoEm;
    private List<ItemOrcamentoResponse> itens;

    public static OrcamentoResponse fromDomain(Orcamento orcamento) {
        List<ItemOrcamentoResponse> itensDto = orcamento.getItens() != null
                ? orcamento.getItens().stream()
                .map(i -> new ItemOrcamentoResponse(
                        i.getId(),
                        i.getInsumoId(),
                        i.getServicoId(),
                        i.getDescricao(),
                        i.getQuantidade(),
                        i.getPrecoUnitario(),
                        i.getValorTotal()
                ))
                .collect(Collectors.toList())
                : List.of();

        return new OrcamentoResponse(
                orcamento.getId(),
                orcamento.getOrdemServicoId(),
                orcamento.getClienteId(),
                orcamento.getValorTotal(),
                orcamento.getStatus(),
                orcamento.getObservacoes(),
                orcamento.getCriadoEm(),
                orcamento.getRespondidoEm(),
                itensDto
        );
    }
}
