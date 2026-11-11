package com.mecanica.oficina_billing_service.adapters.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcessadoSpringDataRepository extends JpaRepository<EventoProcessadoJpaEntity, String> {
}
