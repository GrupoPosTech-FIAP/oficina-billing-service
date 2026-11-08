package com.mecanica.oficina_billing_service.application.orcamento.gateway;

import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.OrcamentoStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrcamentoGateway {
    Orcamento salvar(Orcamento orcamento);
    Orcamento atualizar(Orcamento orcamento);
    Optional<Orcamento> buscarPorId(String id);
    Optional<Orcamento> buscarPorOrdemServicoId(String ordemServicoId);
    List<Orcamento> listar();
    List<Orcamento> listarPorStatus(Collection<OrcamentoStatus> status);
}
