package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.cobranca.input.CriarCobrancaInput;
import com.mecanica.oficina_billing_service.application.cobranca.output.CobrancaOutput;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;

/**
 * Caso de uso para criação de cobrança com preferência no Checkout Pro do Mercado Pago.
 */
public class CriarCobrancaUseCase {

    private final CobrancaGateway cobrancaGateway;
    private final MercadoPagoGateway mercadoPagoGateway;

    public CriarCobrancaUseCase(CobrancaGateway cobrancaGateway, MercadoPagoGateway mercadoPagoGateway) {
        this.cobrancaGateway = cobrancaGateway;
        this.mercadoPagoGateway = mercadoPagoGateway;
    }

    public CobrancaOutput executar(CriarCobrancaInput input) {
        String referencia = "oficina-billing:dev:" + input.pedidoId();

        var pref = mercadoPagoGateway.criarPreferencia(
                referencia,
                input.descricao(),
                input.valor(),
                input.payerEmail(),
                input.payerName(),
                input.payerCpf(),
                input.returnUrl()
        );

        // Prioriza sandboxInitPoint quando em modo sandbox para testes confiáveis
        String linkPagamento = (mercadoPagoGateway.isSandboxMode() && pref.sandboxInitPoint() != null && !pref.sandboxInitPoint().isBlank())
                ? pref.sandboxInitPoint()
                : pref.initPoint();

        Cobranca cobranca = Cobranca.criar(
                input.pedidoId(),
                input.valor(),
                input.descricao(),
                referencia,
                pref.preferenceId(),
                linkPagamento
        );

        Cobranca salva = cobrancaGateway.salvar(cobranca);
        return CobrancaOutput.fromDomain(salva);
    }
}
