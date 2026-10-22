package com.mecanica.oficina_billing_service.adapters.web;

import com.mecanica.oficina_billing_service.adapters.web.dto.request.AtualizarOrcamentoRequest;
import com.mecanica.oficina_billing_service.adapters.web.dto.request.GerarOrcamentoRequest;
import com.mecanica.oficina_billing_service.adapters.web.dto.response.OrcamentoResponse;
import com.mecanica.oficina_billing_service.application.orcamento.input.AtualizarOrcamentoInput;
import com.mecanica.oficina_billing_service.application.orcamento.input.GerarOrcamentoInput;
import com.mecanica.oficina_billing_service.application.orcamento.input.ItemInsumoInput;
import com.mecanica.oficina_billing_service.application.orcamento.input.ItemOrcamentoInput;
import com.mecanica.oficina_billing_service.application.orcamento.input.ItemServicoInput;
import com.mecanica.oficina_billing_service.application.orcamento.usecase.*;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orcamentos")
@Tag(name = "Orçamentos", description = "Gerenciamento do ciclo de vida de orçamentos (geração, envio para aprovação, atualização e aprovação/rejeição)")
public class OrcamentoController {

    private final GerarOrcamentoUseCase gerarOrcamentoUseCase;
    private final EnviarOrcamentoUseCase enviarOrcamentoUseCase;
    private final AtualizarOrcamentoUseCase atualizarOrcamentoUseCase;
    private final AguardarOrcamentoUseCase aguardarOrcamentoUseCase;
    private final AprovarOrcamentoUseCase aprovarOrcamentoUseCase;
    private final NegarOrcamentoUseCase negarOrcamentoUseCase;
    private final ConsultarOrcamentoUseCase consultarOrcamentoUseCase;

    public OrcamentoController(GerarOrcamentoUseCase gerarOrcamentoUseCase,
                              EnviarOrcamentoUseCase enviarOrcamentoUseCase,
                              AtualizarOrcamentoUseCase atualizarOrcamentoUseCase,
                              AguardarOrcamentoUseCase aguardarOrcamentoUseCase,
                              AprovarOrcamentoUseCase aprovarOrcamentoUseCase,
                              NegarOrcamentoUseCase negarOrcamentoUseCase,
                              ConsultarOrcamentoUseCase consultarOrcamentoUseCase) {
        this.gerarOrcamentoUseCase = gerarOrcamentoUseCase;
        this.enviarOrcamentoUseCase = enviarOrcamentoUseCase;
        this.atualizarOrcamentoUseCase = atualizarOrcamentoUseCase;
        this.aguardarOrcamentoUseCase = aguardarOrcamentoUseCase;
        this.aprovarOrcamentoUseCase = aprovarOrcamentoUseCase;
        this.negarOrcamentoUseCase = negarOrcamentoUseCase;
        this.consultarOrcamentoUseCase = consultarOrcamentoUseCase;
    }

    @PostMapping
    @Operation(summary = "Gerar orçamento", description = "Cria um orçamento vinculado a uma Ordem de Serviço com os itens (insumos e/ou serviços)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orçamento gerado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "OS já possui orçamento ativo")
    })
    public ResponseEntity<OrcamentoResponse> gerarOrcamento(@Valid @RequestBody GerarOrcamentoRequest request) {
        GerarOrcamentoInput input = toInput(request);
        Orcamento orcamento = gerarOrcamentoUseCase.executar(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrcamentoResponse.fromDomain(orcamento));
    }

    @PatchMapping("/{id}/enviar")
    @Operation(summary = "Enviar orçamento para aprovação", description = "Transição do orçamento: PENDENTE → ENVIADO. Notifica o cliente para aprovação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orçamento enviado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado"),
            @ApiResponse(responseCode = "409", description = "Orçamento não está em estado PENDENTE")
    })
    public ResponseEntity<OrcamentoResponse> enviarOrcamento(@PathVariable String id) {
        Orcamento orcamento = enviarOrcamentoUseCase.executar(id);
        return ResponseEntity.ok(OrcamentoResponse.fromDomain(orcamento));
    }

    @PatchMapping("/{id}/aguardar")
    @Operation(summary = "Aguardar resposta/ajustes do cliente", description = "Transição: ENVIADO → AGUARDANDO (cliente solicitou negociação ou ajustes)")
    public ResponseEntity<OrcamentoResponse> aguardarOrcamento(@PathVariable String id) {
        Orcamento orcamento = aguardarOrcamentoUseCase.executar(id);
        return ResponseEntity.ok(OrcamentoResponse.fromDomain(orcamento));
    }

    @PatchMapping("/{id}/aprovar")
    @Operation(summary = "Aprovar orçamento", description = "Transição: ENVIADO|AGUARDANDO → APROVADO")
    public ResponseEntity<OrcamentoResponse> aprovarOrcamento(@PathVariable String id) {
        Orcamento orcamento = aprovarOrcamentoUseCase.executar(id);
        return ResponseEntity.ok(OrcamentoResponse.fromDomain(orcamento));
    }

    @PatchMapping("/{id}/negar")
    @Operation(summary = "Negar orçamento", description = "Transição: ENVIADO|AGUARDANDO → NEGADO")
    public ResponseEntity<OrcamentoResponse> negarOrcamento(@PathVariable String id) {
        Orcamento orcamento = negarOrcamentoUseCase.executar(id);
        return ResponseEntity.ok(OrcamentoResponse.fromDomain(orcamento));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar orçamento", description = "Substitui os itens do orçamento quando em estado AGUARDANDO ou PENDENTE")
    public ResponseEntity<OrcamentoResponse> atualizarOrcamento(@PathVariable String id,
                                                                @Valid @RequestBody AtualizarOrcamentoRequest request) {
        AtualizarOrcamentoInput input = toInput(request);
        Orcamento orcamento = atualizarOrcamentoUseCase.executar(id, input);
        return ResponseEntity.ok(OrcamentoResponse.fromDomain(orcamento));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar orçamento por ID")
    public ResponseEntity<OrcamentoResponse> buscarPorId(@PathVariable String id) {
        Orcamento orcamento = consultarOrcamentoUseCase.buscarPorId(id);
        return ResponseEntity.ok(OrcamentoResponse.fromDomain(orcamento));
    }

    @GetMapping("/ordem-servico/{ordemServicoId}")
    @Operation(summary = "Buscar orçamento por ID da Ordem de Serviço")
    public ResponseEntity<OrcamentoResponse> buscarPorOrdemServicoId(@PathVariable String ordemServicoId) {
        Orcamento orcamento = consultarOrcamentoUseCase.buscarPorOrdemServicoId(ordemServicoId);
        return ResponseEntity.ok(OrcamentoResponse.fromDomain(orcamento));
    }

    @GetMapping
    @Operation(summary = "Listar todos os orçamentos")
    public ResponseEntity<List<OrcamentoResponse>> listar() {
        List<OrcamentoResponse> responses = consultarOrcamentoUseCase.listar().stream()
                .map(OrcamentoResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private GerarOrcamentoInput toInput(GerarOrcamentoRequest request) {
        List<ItemOrcamentoInput> itens = request.getItens() != null
                ? request.getItens().stream()
                .map(i -> new ItemOrcamentoInput(i.getDescricao(), i.getQuantidade(), i.getPrecoUnitario(), i.getInsumoId(), i.getServicoId()))
                .collect(Collectors.toList())
                : null;

        List<ItemInsumoInput> insumos = request.getInsumos() != null
                ? request.getInsumos().stream()
                .map(i -> new ItemInsumoInput(i.getInsumoId(), i.getQuantidade(), i.getPrecoUnitario(), i.getDescricao()))
                .collect(Collectors.toList())
                : null;

        List<ItemServicoInput> servicos = request.getServicos() != null
                ? request.getServicos().stream()
                .map(s -> new ItemServicoInput(s.getServicoId(), s.getQuantidade(), s.getPrecoUnitario(), s.getDescricao()))
                .collect(Collectors.toList())
                : null;

        return new GerarOrcamentoInput(
                request.getOrdemServicoId(),
                request.getClienteId(),
                itens,
                insumos,
                servicos,
                request.getObservacoes()
        );
    }

    private AtualizarOrcamentoInput toInput(AtualizarOrcamentoRequest request) {
        List<ItemOrcamentoInput> itens = request.getItens() != null
                ? request.getItens().stream()
                .map(i -> new ItemOrcamentoInput(i.getDescricao(), i.getQuantidade(), i.getPrecoUnitario(), i.getInsumoId(), i.getServicoId()))
                .collect(Collectors.toList())
                : null;

        List<ItemInsumoInput> insumos = request.getInsumos() != null
                ? request.getInsumos().stream()
                .map(i -> new ItemInsumoInput(i.getInsumoId(), i.getQuantidade(), i.getPrecoUnitario(), i.getDescricao()))
                .collect(Collectors.toList())
                : null;

        List<ItemServicoInput> servicos = request.getServicos() != null
                ? request.getServicos().stream()
                .map(s -> new ItemServicoInput(s.getServicoId(), s.getQuantidade(), s.getPrecoUnitario(), s.getDescricao()))
                .collect(Collectors.toList())
                : null;

        return new AtualizarOrcamentoInput(
                itens,
                insumos,
                servicos,
                request.getObservacoes()
        );
    }
}
