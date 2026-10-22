package com.mecanica.oficina_billing_service.adapters.persistence;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CobrancaJpaGateway implements CobrancaGateway {

    private final CobrancaRepository repository;

    public CobrancaJpaGateway(CobrancaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Cobranca salvar(Cobranca cobranca) {
        CobrancaEntity entity = toEntity(cobranca);
        CobrancaEntity salvo = repository.save(entity);
        return toDomain(salvo);
    }

    @Override
    @Transactional
    public Cobranca atualizar(Cobranca cobranca) {
        CobrancaEntity entity = toEntity(cobranca);
        CobrancaEntity salvo = repository.save(entity);
        return toDomain(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cobranca> buscarPorId(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cobranca> buscarPorPedidoId(String pedidoId) {
        return repository.findByPedidoId(pedidoId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cobranca> buscarPorReferencia(String referencia) {
        return repository.findByReferencia(referencia).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cobranca> buscarPorPreferenceId(String preferenceId) {
        return repository.findByPreferenceId(preferenceId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cobranca> listar() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private CobrancaEntity toEntity(Cobranca domain) {
        CobrancaEntity entity = new CobrancaEntity();
        entity.setId(domain.getId());
        entity.setPedidoId(domain.getPedidoId());
        entity.setValor(domain.getValor());
        entity.setDescricao(domain.getDescricao());
        entity.setReferencia(domain.getReferencia());
        entity.setPreferenceId(domain.getPreferenceId());
        entity.setInitPoint(domain.getInitPoint());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : CobrancaStatus.PENDENTE.name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private Cobranca toDomain(CobrancaEntity entity) {
        CobrancaStatus status;
        try {
            status = entity.getStatus() != null ? CobrancaStatus.valueOf(entity.getStatus()) : CobrancaStatus.PENDENTE;
        } catch (IllegalArgumentException e) {
            status = CobrancaStatus.PENDENTE;
        }

        return Cobranca.reconstituir(
                entity.getId(),
                entity.getPedidoId(),
                entity.getValor(),
                entity.getDescricao(),
                entity.getReferencia(),
                entity.getPreferenceId(),
                entity.getInitPoint(),
                status,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
