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
    /** Id do pagamento no Mercado Pago, conhecido quando o webhook confirma o pagamento; usado no estorno. */
    private final Long pagamentoId;

    public Cobranca(UUID id, String pedidoId, BigDecimal valor, String descricao,
                    String referencia, String preferenceId, String initPoint,
                    CobrancaStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, pedidoId, valor, descricao, referencia, preferenceId, initPoint, status, createdAt, updatedAt, null);
    }

    public Cobranca(UUID id, String pedidoId, BigDecimal valor, String descricao,
                    String referencia, String preferenceId, String initPoint,
                    CobrancaStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt, Long pagamentoId) {
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
        this.pagamentoId = pagamentoId;
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

    public static Cobranca reconstituir(UUID id, String pedidoId, BigDecimal valor, String descricao,
                                        String referencia, String preferenceId, String initPoint,
                                        CobrancaStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                        Long pagamentoId) {
        return new Cobranca(id, pedidoId, valor, descricao, referencia, preferenceId, initPoint, status, createdAt, updatedAt, pagamentoId);
    }

    public Cobranca marcarComoPago() {
        return marcarComoPago(pagamentoId);
    }

    public Cobranca marcarComoPago(Long pagamentoId) {
        if (this.status == CobrancaStatus.PAGO) {
            return this; // idempotente
        }
        if (!aceitaPagamento()) {
            throw new IllegalStateException("Não é possível pagar uma cobrança com status " + this.status);
        }
        return new Cobranca(id, pedidoId, valor, descricao, referencia, preferenceId, initPoint,
                CobrancaStatus.PAGO, createdAt, OffsetDateTime.now(), pagamentoId);
    }

    /**
     * Só a cobrança {@code PENDENTE} aceita pagamento. Recusada ou cancelada (expirada), a Saga já
     * cancelou a OS e devolveu o estoque, então um pagamento que chegue depois precisa ser devolvido.
     */
    public boolean aceitaPagamento() {
        return this.status == CobrancaStatus.PENDENTE;
    }

    /** Registra o reembolso de um pagamento aprovado depois que a cobrança foi recusada ou cancelada. */
    public Cobranca estornarPagamentoTardio(Long pagamentoId) {
        if (this.status != CobrancaStatus.CANCELADO && this.status != CobrancaStatus.RECUSADO) {
            throw new IllegalStateException("Estorno de pagamento tardio exige cobrança recusada ou cancelada, não " + this.status);
        }
        return new Cobranca(id, pedidoId, valor, descricao, referencia, preferenceId, initPoint,
                CobrancaStatus.ESTORNADO, createdAt, OffsetDateTime.now(),
                Objects.requireNonNull(pagamentoId, "ID do pagamento é obrigatório"));
    }

    public Cobranca marcarComoRecusado() {
        if (this.status == CobrancaStatus.RECUSADO) {
            return this;
        }
        return new Cobranca(id, pedidoId, valor, descricao, referencia, preferenceId, initPoint,
                CobrancaStatus.RECUSADO, createdAt, OffsetDateTime.now(), pagamentoId);
    }

    public Cobranca estornar() {
        if (this.status != CobrancaStatus.PAGO) {
            throw new IllegalStateException("Apenas cobranças pagas podem ser estornadas");
        }
        return new Cobranca(id, pedidoId, valor, descricao, referencia, preferenceId, initPoint,
                CobrancaStatus.ESTORNADO, createdAt, OffsetDateTime.now(), pagamentoId);
    }

    public Cobranca cancelar() {
        if (this.status == CobrancaStatus.PAGO) {
            throw new IllegalStateException("Cobrança já paga não pode ser cancelada diretamente");
        }
        return new Cobranca(id, pedidoId, valor, descricao, referencia, preferenceId, initPoint,
                CobrancaStatus.CANCELADO, createdAt, OffsetDateTime.now(), pagamentoId);
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

    public Long getPagamentoId() {
        return pagamentoId;
    }
}
