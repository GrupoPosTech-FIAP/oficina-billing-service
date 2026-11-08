package com.mecanica.oficina_billing_service.adapters.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrcamentoJpaRepository extends JpaRepository<OrcamentoEntity, String> {
    Optional<OrcamentoEntity> findByOrdemServicoId(String ordemServicoId);
    List<OrcamentoEntity> findByStatusIn(Collection<OrcamentoStatus> status);
}
