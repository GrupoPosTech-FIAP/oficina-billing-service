package com.mecanica.oficina_billing_service.application.cobranca.gateway;

import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CobrancaGateway {
    Cobranca salvar(Cobranca cobranca);
    Cobranca atualizar(Cobranca cobranca);
    Optional<Cobranca> buscarPorId(UUID id);
    Optional<Cobranca> buscarPorPedidoId(String pedidoId);
    Optional<Cobranca> buscarPorReferencia(String referencia);
    Optional<Cobranca> buscarPorPreferenceId(String preferenceId);
    List<Cobranca> listar();
}
