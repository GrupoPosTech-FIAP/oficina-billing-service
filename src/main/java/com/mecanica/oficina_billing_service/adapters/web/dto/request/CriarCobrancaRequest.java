package com.mecanica.oficina_billing_service.adapters.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Dados de requisição para geração de cobrança no Checkout Pro.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para criação de cobrança com preferência de pagamento no Mercado Pago")
public class CriarCobrancaRequest {

    @NotBlank(message = "O ID do pedido/ordem de serviço é obrigatório")
    @Schema(description = "Identificador da ordem de serviço ou pedido", example = "OS-2026-001")
    private String pedidoId;

    @NotBlank(message = "A descrição da cobrança é obrigatória")
    @Schema(description = "Descrição dos serviços ou produtos", example = "Troca de pastilhas de freio e óleo do motor")
    private String descricao;

    @NotNull(message = "O valor da cobrança é obrigatório")
    @Positive(message = "O valor deve ser maior que zero")
    @Schema(description = "Valor total da cobrança em BRL", example = "250.00")
    private BigDecimal valor;

    @Email(message = "E-mail do pagador inválido")
    @Schema(description = "E-mail do pagador para pré-preenchimento e recibo", example = "cliente@exemplo.com.br")
    private String payerEmail;

    @Schema(description = "Nome do pagador", example = "Carlos Silva")
    private String payerName;

    @Schema(description = "CPF do pagador (apenas números ou formatado)", example = "123.456.789-00")
    private String payerCpf;

    @Schema(description = "URL customizada de retorno após pagamento (sobrescreve o default configurado no serviço)", example = "https://minha-aplicacao.com.br/pedidos/OS-2026-001/retorno")
    private String returnUrl;

    public CriarCobrancaRequest(String pedidoId, String descricao, BigDecimal valor) {
        this(pedidoId, descricao, valor, null, null, null, null);
    }

    public CriarCobrancaRequest(String pedidoId, String descricao, BigDecimal valor,
                                String payerEmail, String payerName, String payerCpf) {
        this(pedidoId, descricao, valor, payerEmail, payerName, payerCpf, null);
    }
}
