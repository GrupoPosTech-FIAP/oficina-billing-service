package com.mecanica.oficina_billing_service.application.cobranca.output;

import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CobrancaOutput(
        UUID id,
        String pedidoId,
        BigDecimal valor,
        String descricao,
        String referencia,
        String preferenceId,
        String initPoint,
        CobrancaStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CobrancaOutput fromDomain(Cobranca c) {
        return new CobrancaOutput(
                c.getId(),
                c.getPedidoId(),
                c.getValor(),
                c.getDescricao(),
                c.getReferencia(),
                c.getPreferenceId(),
                c.getInitPoint(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
