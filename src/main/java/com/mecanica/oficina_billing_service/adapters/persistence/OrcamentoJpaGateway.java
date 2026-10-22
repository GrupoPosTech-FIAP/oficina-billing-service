package com.mecanica.oficina_billing_service.adapters.persistence;

import com.mecanica.oficina_billing_service.application.orcamento.gateway.OrcamentoGateway;
import com.mecanica.oficina_billing_service.domain.orcamento.ItemOrcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OrcamentoJpaGateway implements OrcamentoGateway {

    private final OrcamentoJpaRepository repository;

    public OrcamentoJpaGateway(OrcamentoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Orcamento salvar(Orcamento orcamento) {
        OrcamentoEntity entity = toEntity(orcamento);
        OrcamentoEntity salvo = repository.save(entity);
        return toDomain(salvo);
    }

    @Override
    @Transactional
    public Orcamento atualizar(Orcamento orcamento) {
        OrcamentoEntity entity = toEntity(orcamento);
        OrcamentoEntity salvo = repository.save(entity);
        return toDomain(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Orcamento> buscarPorId(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Orcamento> buscarPorOrdemServicoId(String ordemServicoId) {
        return repository.findByOrdemServicoId(ordemServicoId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Orcamento> listar() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private OrcamentoEntity toEntity(Orcamento domain) {
        OrcamentoEntity entity = new OrcamentoEntity();
        entity.setId(domain.getId());
        entity.setOrdemServicoId(domain.getOrdemServicoId());
        entity.setClienteId(domain.getClienteId());
        entity.setValorTotal(domain.getValorTotal());
        entity.setStatus(domain.getStatus());
        entity.setObservacoes(domain.getObservacoes());
        entity.setCriadoEm(domain.getCriadoEm());
        entity.setRespondidoEm(domain.getRespondidoEm());

        if (domain.getItens() != null) {
            for (ItemOrcamento itemDomain : domain.getItens()) {
                ItemOrcamentoEntity itemEntity = new ItemOrcamentoEntity();
                itemEntity.setId(itemDomain.getId());
                itemEntity.setInsumoId(itemDomain.getInsumoId());
                itemEntity.setServicoId(itemDomain.getServicoId());
                itemEntity.setDescricao(itemDomain.getDescricao());
                itemEntity.setQuantidade(itemDomain.getQuantidade());
                itemEntity.setPrecoUnitario(itemDomain.getPrecoUnitario());
                itemEntity.setValorTotal(itemDomain.getValorTotal());
                entity.adicionarItem(itemEntity);
            }
        }

        return entity;
    }

    private Orcamento toDomain(OrcamentoEntity entity) {
        List<ItemOrcamento> itens = entity.getItens().stream()
                .map(i -> new ItemOrcamento(
                        i.getId(),
                        i.getInsumoId(),
                        i.getServicoId(),
                        i.getDescricao(),
                        i.getQuantidade(),
                        i.getPrecoUnitario()
                ))
                .collect(Collectors.toList());

        return Orcamento.reconstituir(
                entity.getId(),
                entity.getOrdemServicoId(),
                entity.getClienteId(),
                itens,
                entity.getStatus(),
                entity.getObservacoes(),
                entity.getCriadoEm(),
                entity.getRespondidoEm()
        );
    }
}
