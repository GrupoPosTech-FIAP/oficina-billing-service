package com.mecanica.oficina_billing_service.application.orcamento.gateway;

import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;

import java.util.List;
import java.util.Optional;

public interface OrcamentoGateway {
    Orcamento salvar(Orcamento orcamento);
    Orcamento atualizar(Orcamento orcamento);
    Optional<Orcamento> buscarPorId(String id);
    Optional<Orcamento> buscarPorOrdemServicoId(String ordemServicoId);
    List<Orcamento> listar();
}
