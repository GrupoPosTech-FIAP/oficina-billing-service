package com.mecanica.oficina_billing_service.adapters.persistence;

import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orcamentos",
        indexes = {
                @Index(name = "idx_orcamentos_os_id", columnList = "ordem_servico_id")
        })
public class OrcamentoEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "ordem_servico_id", nullable = false, length = 64)
    private String ordemServicoId;

    @Column(name = "cliente_id", length = 64)
    private String clienteId;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrcamentoStatus status;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "respondido_em")
    private LocalDateTime respondidoEm;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemOrcamentoEntity> itens = new ArrayList<>();

    public void adicionarItem(ItemOrcamentoEntity item) {
        itens.add(item);
        item.setOrcamento(this);
    }
}
