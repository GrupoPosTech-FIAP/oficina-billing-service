package com.mecanica.oficina_billing_service.domain.cobranca;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class Cobranca {

    private final UUID id;
    private final String pedidoId;
    private final BigDecimal valor;
    private final String descricao;
    private final String referencia;
    private final String preferenceId;
    private final String initPoint;
    private final CobrancaStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public Cobranca(UUID id, String pedidoId, BigDecimal valor, String descricao,
                    String referencia, String preferenceId, String initPoint,
                    CobrancaStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.pedidoId = Objects.requireNonNull(pedidoId, "Pedido ID é obrigatório");
        this.valor = Objects.requireNonNull(valor, "Valor é obrigatório");
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da cobrança deve ser maior que zero");
        }
        this.descricao = descricao;
        this.referencia = Objects.requireNonNull(referencia, "Referência externa é obrigatória");
        this.preferenceId = Objects.requireNonNull(preferenceId, "Preference ID do Mercado Pago é obrigatório");
        this.initPoint = Objects.requireNonNull(initPoint, "Link de pagamento (initPoint) é obrigatório");
        this.status = status != null ? status : CobrancaStatus.PENDENTE;
        this.createdAt = createdAt != null ? createdAt : OffsetDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : OffsetDateTime.now();
    }

    public static Cobranca criar(String pedidoId, BigDecimal valor, String descricao,
                                 String referencia, String preferenceId, String initPoint) {
        var now = OffsetDateTime.now();
        return new Cobranca(UUID.randomUUID(), pedidoId, valor, descricao,
                referencia, preferenceId, initPoint, CobrancaStatus.PENDENTE, now, now);
    }

    public static Cobranca reconstituir(UUID id, String pedidoId, BigDecimal valor, String descricao,
                                        String referencia, String preferenceId, String initPoint,
                                        CobrancaStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new Cobranca(id, pedidoId, valor, descricao, referencia, preferenceId, initPoint, status, createdAt, updatedAt);
    }

    public Cobranca marcarComoPago() {
        if (this.status == CobrancaStatus.PAGO) {
            return this; // idempotente
        }
        if (this.status == CobrancaStatus.CANCELADO || this.status == CobrancaStatus.ESTORNADO) {
            throw new IllegalStateException("Não é possível pagar uma cobrança com status " + this.status);
        }
        return new Cobranca(id, pedidoId, valor, descricao, referencia, preferenceId, initPoint,
                CobrancaStatus.PAGO, createdAt, OffsetDateTime.now());
    }

    public Cobranca marcarComoRecusado() {
        if (this.status == CobrancaStatus.RECUSADO) {
            return this;
        }
        return new Cobranca(id, pedidoId, valor, descricao, referencia, preferenceId, initPoint,
                CobrancaStatus.RECUSADO, createdAt, OffsetDateTime.now());
    }

    public Cobranca estornar() {
        if (this.status != CobrancaStatus.PAGO) {
            throw new IllegalStateException("Apenas cobranças pagas podem ser estornadas");
        }
        return new Cobranca(id, pedidoId, valor, descricao, referencia, preferenceId, initPoint,
                CobrancaStatus.ESTORNADO, createdAt, OffsetDateTime.now());
    }

    public Cobranca cancelar() {
        if (this.status == CobrancaStatus.PAGO) {
            throw new IllegalStateException("Cobrança já paga não pode ser cancelada diretamente");
        }
        return new Cobranca(id, pedidoId, valor, descricao, referencia, preferenceId, initPoint,
                CobrancaStatus.CANCELADO, createdAt, OffsetDateTime.now());
    }

    public UUID getId() {
        return id;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public String getInitPoint() {
        return initPoint;
    }

    public CobrancaStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
