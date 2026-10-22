package com.mecanica.oficina_billing_service.adapters.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cobrancas",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cobrancas_referencia", columnNames = "referencia"),
                @UniqueConstraint(name = "uk_cobrancas_preference_id", columnNames = "preference_id")
        })
public class CobrancaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "pedido_id", nullable = false, length = 100)
    private String pedidoId;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal valor;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false, length = 200)
    private String referencia;

    @Column(name = "preference_id", nullable = false, length = 120)
    private String preferenceId;

    @Column(name = "init_point", nullable = false, length = 500)
    private String initPoint;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = "PENDENTE";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // getters/setters omitidos
}