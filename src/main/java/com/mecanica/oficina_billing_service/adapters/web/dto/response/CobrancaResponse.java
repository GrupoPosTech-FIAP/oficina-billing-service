package com.mecanica.oficina_billing_service.adapters.web.dto.response;

import com.mecanica.oficina_billing_service.application.cobranca.output.CobrancaOutput;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CobrancaResponse(
        UUID id,
        String pedidoId,
        BigDecimal valor,
        String descricao,
        String referencia,
        String preferenceId,
        String linkPagamento,
        CobrancaStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CobrancaResponse fromOutput(CobrancaOutput output) {
        return new CobrancaResponse(
                output.id(),
                output.pedidoId(),
                output.valor(),
                output.descricao(),
                output.referencia(),
                output.preferenceId(),
                output.initPoint(),
                output.status(),
                output.createdAt(),
                output.updatedAt()
        );
    }
}
