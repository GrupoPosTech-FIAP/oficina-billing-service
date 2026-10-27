package com.mecanica.oficina_billing_service.adapters.web.dto.response;

import com.mecanica.oficina_billing_service.application.cobranca.output.CobrancaOutput;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resposta de criação de cobrança no Checkout Pro do Mercado Pago.
 * Fornece a URL direta de pagamento (linkPagamento) para o usuário acessar e pagar.
 */
@Schema(description = "Resposta de criação de cobrança com link de pagamento direto para Checkout Pro")
public record CriarCobrancaResponse(
        @Schema(description = "ID único da cobrança no banco de dados da oficina", example = "d3b07384-d113-40e9-b5f7-41804c7a6fdf")
        UUID cobrancaId,

        @Schema(description = "Identificador da ordem de serviço / pedido", example = "OS-2026-001")
        String pedidoId,

        @Schema(description = "Valor total da cobrança em BRL", example = "250.00")
        BigDecimal valor,

        @Schema(description = "Link direto para o Checkout Pro no Mercado Pago (redirecionar usuário para esta URL)", example = "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=123")
        String linkPagamento,

        @Schema(description = "Referência externa para reconciliação do pedido", example = "oficina-billing:dev:OS-2026-001")
        String referencia,

        @Schema(description = "Status atual da cobrança", example = "PENDENTE")
        CobrancaStatus status
) {
    public static CriarCobrancaResponse fromOutput(CobrancaOutput output) {
        return new CriarCobrancaResponse(
                output.id(),
                output.pedidoId(),
                output.valor(),
                output.initPoint(),
                output.referencia(),
                output.status()
        );
    }
}
