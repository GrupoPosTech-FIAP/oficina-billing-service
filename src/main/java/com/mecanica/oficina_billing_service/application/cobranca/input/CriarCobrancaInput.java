package com.mecanica.oficina_billing_service.application.cobranca.input;

import java.math.BigDecimal;

public record CriarCobrancaInput(
        String pedidoId,
        String descricao,
        BigDecimal valor
) {}
