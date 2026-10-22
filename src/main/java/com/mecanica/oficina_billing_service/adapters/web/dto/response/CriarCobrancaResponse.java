package com.mecanica.oficina_billing_service.adapters.web.dto.response;

import com.mecanica.oficina_billing_service.application.cobranca.output.CobrancaOutput;

import java.util.UUID;

public record CriarCobrancaResponse(UUID cobrancaId, String linkPagamento, String referencia) {

    public static CriarCobrancaResponse fromOutput(CobrancaOutput output) {
        return new CriarCobrancaResponse(output.id(), output.initPoint(), output.referencia());
    }
}
