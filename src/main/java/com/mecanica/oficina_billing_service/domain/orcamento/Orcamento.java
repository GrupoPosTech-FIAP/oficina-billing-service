package com.mecanica.oficina_billing_service.domain.orcamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Orcamento {

    private final String id;
    private final String ordemServicoId;
    private final String clienteId;
    private final List<ItemOrcamento> itens;
    private final BigDecimal valorTotal;
    private final OrcamentoStatus status;
    private final String observacoes;
    private final LocalDateTime criadoEm;
    private final LocalDateTime respondidoEm;

    public Orcamento(String ordemServicoId, String clienteId, List<ItemOrcamento> itens, String observacoes) {
        this(UUID.randomUUID().toString(), ordemServicoId, clienteId, itens, OrcamentoStatus.PENDENTE, observacoes, LocalDateTime.now(), null);
    }

    public Orcamento(String id, String ordemServicoId, String clienteId, List<ItemOrcamento> itens,
                     OrcamentoStatus status, String observacoes, LocalDateTime criadoEm, LocalDateTime respondidoEm) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.ordemServicoId = Objects.requireNonNull(ordemServicoId, "ID da ordem de serviço é obrigatório");
        this.clienteId = clienteId;
        Objects.requireNonNull(itens, "Itens são obrigatórios");
        if (itens.isEmpty()) {
            throw new IllegalArgumentException("Orçamento deve ter ao menos um item");
        }
        this.itens = List.copyOf(itens);
        this.valorTotal = calcularTotal(itens);
        this.status = status != null ? status : OrcamentoStatus.PENDENTE;
        this.observacoes = observacoes;
        this.criadoEm = criadoEm != null ? criadoEm : LocalDateTime.now();
        this.respondidoEm = respondidoEm;
    }

    public static Orcamento criar(String ordemServicoId, String clienteId, List<ItemOrcamento> itens, String observacoes) {
        return new Orcamento(ordemServicoId, clienteId, itens, observacoes);
    }

    public static Orcamento reconstituir(String id, String ordemServicoId, String clienteId, List<ItemOrcamento> itens,
                                         OrcamentoStatus status, String observacoes, LocalDateTime criadoEm, LocalDateTime respondidoEm) {
        return new Orcamento(id, ordemServicoId, clienteId, itens, status, observacoes, criadoEm, respondidoEm);
    }

    public Orcamento enviar() {
        if (status != OrcamentoStatus.PENDENTE) {
            throw new IllegalStateException("Orçamento deve estar PENDENTE para ser enviado");
        }
        return new Orcamento(id, ordemServicoId, clienteId, itens, OrcamentoStatus.ENVIADO, observacoes, criadoEm, null);
    }

    public Orcamento aguardar() {
        if (status != OrcamentoStatus.ENVIADO) {
            throw new IllegalStateException("Orçamento deve estar ENVIADO para entrar em aguardo");
        }
        return new Orcamento(id, ordemServicoId, clienteId, itens, OrcamentoStatus.AGUARDANDO, observacoes, criadoEm, null);
    }

    public Orcamento aprovar() {
        if (status != OrcamentoStatus.ENVIADO && status != OrcamentoStatus.AGUARDANDO) {
            throw new IllegalStateException("Orçamento deve estar ENVIADO ou AGUARDANDO para ser aprovado");
        }
        return new Orcamento(id, ordemServicoId, clienteId, itens, OrcamentoStatus.APROVADO, observacoes, criadoEm, LocalDateTime.now());
    }

    public Orcamento negar() {
        if (status != OrcamentoStatus.ENVIADO && status != OrcamentoStatus.AGUARDANDO) {
            throw new IllegalStateException("Orçamento deve estar ENVIADO ou AGUARDANDO para ser negado");
        }
        return new Orcamento(id, ordemServicoId, clienteId, itens, OrcamentoStatus.NEGADO, observacoes, criadoEm, LocalDateTime.now());
    }

    public Orcamento atualizar(List<ItemOrcamento> novosItens, String novasObservacoes) {
        if (status != OrcamentoStatus.AGUARDANDO && status != OrcamentoStatus.PENDENTE) {
            throw new IllegalStateException("Orçamento só pode ser atualizado se estiver PENDENTE ou AGUARDANDO");
        }
        return new Orcamento(id, ordemServicoId, clienteId, novosItens, status, novasObservacoes, criadoEm, respondidoEm);
    }

    private static BigDecimal calcularTotal(List<ItemOrcamento> itens) {
        return itens.stream()
                .map(ItemOrcamento::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getId() {
        return id;
    }

    public String getOrdemServicoId() {
        return ordemServicoId;
    }

    public String getClienteId() {
        return clienteId;
    }

    public List<ItemOrcamento> getItens() {
        return itens;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public OrcamentoStatus getStatus() {
        return status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getRespondidoEm() {
        return respondidoEm;
    }
}
