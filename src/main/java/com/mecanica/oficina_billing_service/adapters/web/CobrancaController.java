package com.mecanica.oficina_billing_service.adapters.web;

import com.mecanica.oficina_billing_service.adapters.web.dto.request.CriarCobrancaRequest;
import com.mecanica.oficina_billing_service.adapters.web.dto.response.CobrancaResponse;
import com.mecanica.oficina_billing_service.adapters.web.dto.response.CriarCobrancaResponse;
import com.mecanica.oficina_billing_service.application.cobranca.input.CriarCobrancaInput;
import com.mecanica.oficina_billing_service.application.cobranca.output.CobrancaOutput;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ConsultarCobrancaUseCase;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.CriarCobrancaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller REST para gestão de cobranças e integração com Checkout Pro do Mercado Pago.
 */
@RestController
@RequestMapping("/cobrancas")
@Tag(name = "Cobranças", description = "Gerenciamento de cobranças e geração de URL de pagamento para Checkout Pro do Mercado Pago")
public class CobrancaController {

    private final CriarCobrancaUseCase criarCobrancaUseCase;
    private final ConsultarCobrancaUseCase consultarCobrancaUseCase;

    public CobrancaController(CriarCobrancaUseCase criarCobrancaUseCase,
                             ConsultarCobrancaUseCase consultarCobrancaUseCase) {
        this.criarCobrancaUseCase = criarCobrancaUseCase;
        this.consultarCobrancaUseCase = consultarCobrancaUseCase;
    }

    @PostMapping
    @Operation(summary = "Criar cobrança no Mercado Pago", description = "Gera uma preferência de pagamento no Checkout Pro e retorna a URL direta de pagamento (linkPagamento)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cobrança criada com link direto para o Checkout Pro"),
            @ApiResponse(responseCode = "400", description = "Dados da cobrança inválidos"),
            @ApiResponse(responseCode = "500", description = "Erro na integração com Mercado Pago")
    })
    public ResponseEntity<CriarCobrancaResponse> criar(@Valid @RequestBody CriarCobrancaRequest req) {
        var input = new CriarCobrancaInput(
                req.getPedidoId(),
                req.getDescricao(),
                req.getValor(),
                req.getPayerEmail(),
                req.getPayerName(),
                req.getPayerCpf(),
                req.getReturnUrl()
        );
        CobrancaOutput output = criarCobrancaUseCase.executar(input);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CriarCobrancaResponse.fromOutput(output));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cobrança por ID")
    public ResponseEntity<CobrancaResponse> buscarPorId(@PathVariable UUID id) {
        CobrancaOutput output = consultarCobrancaUseCase.buscarPorId(id);
        return ResponseEntity.ok(CobrancaResponse.fromOutput(output));
    }

    @GetMapping("/pedido/{pedidoId}")
    @Operation(summary = "Buscar cobrança por ID do pedido / ordem de serviço")
    public ResponseEntity<CobrancaResponse> buscarPorPedidoId(@PathVariable String pedidoId) {
        CobrancaOutput output = consultarCobrancaUseCase.buscarPorPedidoId(pedidoId);
        return ResponseEntity.ok(CobrancaResponse.fromOutput(output));
    }

    @GetMapping
    @Operation(summary = "Listar todas as cobranças")
    public ResponseEntity<List<CobrancaResponse>> listar() {
        List<CobrancaResponse> response = consultarCobrancaUseCase.listar().stream()
                .map(CobrancaResponse::fromOutput)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
