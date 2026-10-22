package com.mecanica.oficina_billing_service.application.cobranca.usecase;

import com.mecanica.oficina_billing_service.application.cobranca.gateway.CobrancaGateway;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.cobranca.input.CriarCobrancaInput;
import com.mecanica.oficina_billing_service.application.cobranca.output.CobrancaOutput;
import com.mecanica.oficina_billing_service.domain.cobranca.Cobranca;

public class CriarCobrancaUseCase {

    private final CobrancaGateway cobrancaGateway;
    private final MercadoPagoGateway mercadoPagoGateway;

    public CriarCobrancaUseCase(CobrancaGateway cobrancaGateway, MercadoPagoGateway mercadoPagoGateway) {
        this.cobrancaGateway = cobrancaGateway;
        this.mercadoPagoGateway = mercadoPagoGateway;
    }

    public CobrancaOutput executar(CriarCobrancaInput input) {
        String referencia = "oficina-billing:dev:" + input.pedidoId();

        var pref = mercadoPagoGateway.criarPreferencia(referencia, input.descricao(), input.valor());

        Cobranca cobranca = Cobranca.criar(
                input.pedidoId(),
                input.valor(),
                input.descricao(),
                referencia,
                pref.preferenceId(),
                pref.initPoint()
        );

        Cobranca salva = cobrancaGateway.salvar(cobranca);
        return CobrancaOutput.fromDomain(salva);
    }
}
