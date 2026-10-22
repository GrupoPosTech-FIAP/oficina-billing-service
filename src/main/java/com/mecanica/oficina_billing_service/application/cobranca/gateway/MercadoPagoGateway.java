package com.mecanica.oficina_billing_service.application.cobranca.gateway;

import java.math.BigDecimal;
import java.util.Optional;

public interface MercadoPagoGateway {

    record PreferenciaPagamento(String preferenceId, String initPoint) {}

    record DetalhePagamento(Long paymentId, String status, String statusDetail, String externalReference) {}

    PreferenciaPagamento criarPreferencia(String referencia, String descricao, BigDecimal valor);

    Optional<DetalhePagamento> consultarPagamento(Long paymentId);
}
