package com.mecanica.oficina_billing_service.application.cobranca.gateway;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Gateway de integração com os serviços de Checkout Pro e Pagamentos do Mercado Pago.
 */
public interface MercadoPagoGateway {

    /**
     * Informações da preferência criada no Mercado Pago.
     */
    record PreferenciaPagamento(String preferenceId, String initPoint, String sandboxInitPoint) {
        public PreferenciaPagamento(String preferenceId, String initPoint) {
            this(preferenceId, initPoint, initPoint);
        }
    }

    /**
     * Detalhes de um pagamento consultado no Mercado Pago.
     */
    record DetalhePagamento(
            Long paymentId,
            String status,
            String statusDetail,
            String externalReference,
            String paymentMethodId,
            BigDecimal transactionAmount
    ) {
        public DetalhePagamento(Long paymentId, String status, String statusDetail, String externalReference) {
            this(paymentId, status, statusDetail, externalReference, null, null);
        }
    }

    /**
     * Cria preferência simplificada.
     */
    default PreferenciaPagamento criarPreferencia(String referencia, String descricao, BigDecimal valor) {
        return criarPreferencia(referencia, descricao, valor, null, null, null, null);
    }

    /**
     * Cria preferência completa com dados do pagador.
     */
    default PreferenciaPagamento criarPreferencia(String referencia, String descricao, BigDecimal valor,
                                          String payerEmail, String payerName, String payerCpf) {
        return criarPreferencia(referencia, descricao, valor, payerEmail, payerName, payerCpf, null);
    }

    /**
     * Cria preferência completa com dados do pagador e URL customizada de retorno.
     */
    PreferenciaPagamento criarPreferencia(String referencia, String descricao, BigDecimal valor,
                                          String payerEmail, String payerName, String payerCpf, String returnUrl);

    /**
     * Consulta o status atualizado de uma transação pelo paymentId.
     */
    Optional<DetalhePagamento> consultarPagamento(Long paymentId);

    /**
     * Retorna a chave pública para inicialização de componentes no Frontend.
     */
    String getPublicKey();

    /**
     * Indica se o gateway está operando em modo Sandbox.
     */
    boolean isSandboxMode();
}
