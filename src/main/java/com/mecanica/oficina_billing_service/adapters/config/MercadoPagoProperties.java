package com.mecanica.oficina_billing_service.adapters.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriedades de configuração do Mercado Pago para Checkout Pro e Webhooks.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mp")
public class MercadoPagoProperties {

    /**
     * Access Token privado para autenticação nas APIs REST do Mercado Pago (Backend).
     */
    private String accessToken = "";

    /**
     * Public Key para inicialização do SDK MercadoPago.js / Bricks no Frontend.
     */
    private String publicKey = "";

    /**
     * Assinatura secreta para validação do header x-signature recebido nos webhooks.
     */
    private String webhookSecret = "";

    /**
     * URL pública para onde o Mercado Pago enviará notificações assíncronas (Webhooks).
     */
    private String webhookPublicUrl = "";

    /**
     * URL de retorno em caso de pagamento aprovado.
     */
    private String backUrlSuccess = "https://oficina.com.br/pagamentos/sucesso";

    /**
     * URL de retorno em caso de pagamento rejeitado/falho.
     */
    private String backUrlFailure = "https://oficina.com.br/pagamentos/falha";

    /**
     * URL de retorno em caso de pagamento pendente (Pix / Boleto em processamento).
     */
    private String backUrlPending = "https://oficina.com.br/pagamentos/pendente";

    /**
     * Comportamento de redirecionamento automático (ex: "approved").
     */
    private String autoReturn = "approved";

    /**
     * Nome que aparecerá na fatura do cartão do cliente (máx 16 caracteres).
     */
    private String statementDescriptor = "OFICINA";

    /**
     * Define se deve priorizar links em modo Sandbox/Teste.
     */
    private boolean sandboxMode = true;
}
