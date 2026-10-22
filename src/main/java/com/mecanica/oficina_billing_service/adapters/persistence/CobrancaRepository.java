package com.mecanica.oficina_billing_service.adapters.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CobrancaRepository extends JpaRepository<CobrancaEntity, UUID> {
    Optional<CobrancaEntity> findByPedidoId(String pedidoId);
    Optional<CobrancaEntity> findByReferencia(String referencia);
    Optional<CobrancaEntity> findByPreferenceId(String preferenceId);
}
