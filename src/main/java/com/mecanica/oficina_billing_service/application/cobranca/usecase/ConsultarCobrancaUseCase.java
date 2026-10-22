package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.output.CobrancaOutput;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ConsultarCobrancaUseCase {

    private final CobrancaGateway cobrancaGateway;

    public ConsultarCobrancaUseCase(CobrancaGateway cobrancaGateway) {
        this.cobrancaGateway = cobrancaGateway;
    }

    public CobrancaOutput buscarPorId(UUID id) {
        return cobrancaGateway.buscarPorId(id)
                .map(CobrancaOutput::fromDomain)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada para o ID: " + id));
    }

    public CobrancaOutput buscarPorPedidoId(String pedidoId) {
        return cobrancaGateway.buscarPorPedidoId(pedidoId)
                .map(CobrancaOutput::fromDomain)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada para o pedido: " + pedidoId));
    }

    public List<CobrancaOutput> listar() {
        return cobrancaGateway.listar().stream()
                .map(CobrancaOutput::fromDomain)
                .collect(Collectors.toList());
    }
}
