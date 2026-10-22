package com.mecanica.oficina_billing_service.adapters.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mecanica.oficina_billing_service.adapters.web.dto.request.GerarOrcamentoRequest;
import com.mecanica.oficina_billing_service.adapters.web.dto.request.ItemOrcamentoRequest;
import com.mecanica.oficina_billing_service.application.orcamento.input.GerarOrcamentoInput;
import com.mecanica.oficina_billing_service.application.orcamento.usecase.*;
import com.mecanica.oficina_billing_service.domain.orcamento.ItemOrcamento;
import com.mecanica.oficina_billing_service.domain.orcamento.Orcamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private GerarOrcamentoUseCase gerarOrcamentoUseCase;

    @Mock
    private EnviarOrcamentoUseCase enviarOrcamentoUseCase;

    @Mock
    private AtualizarOrcamentoUseCase atualizarOrcamentoUseCase;

    @Mock
    private AguardarOrcamentoUseCase aguardarOrcamentoUseCase;

    @Mock
    private AprovarOrcamentoUseCase aprovarOrcamentoUseCase;

    @Mock
    private NegarOrcamentoUseCase negarOrcamentoUseCase;

    @Mock
    private ConsultarOrcamentoUseCase consultarOrcamentoUseCase;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        OrcamentoController controller = new OrcamentoController(
                gerarOrcamentoUseCase,
                enviarOrcamentoUseCase,
                atualizarOrcamentoUseCase,
                aguardarOrcamentoUseCase,
                aprovarOrcamentoUseCase,
                negarOrcamentoUseCase,
                consultarOrcamentoUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /orcamentos deve retornar 201 com orçamento gerado")
    void deveGerarOrcamentoERetornar201() throws Exception {
        var itemRequest = new ItemOrcamentoRequest("Filtro de Ar", 1, new BigDecimal("45.00"), "ins-1", null);
        var request = new GerarOrcamentoRequest("os-100", "cli-200", List.of(itemRequest), null, null, "Obs");

        var itemDomain = new ItemOrcamento("ins-1", null, "Filtro de Ar", 1, new BigDecimal("45.00"));
        var orcamento = Orcamento.criar("os-100", "cli-200", List.of(itemDomain), "Obs");

        when(gerarOrcamentoUseCase.executar(any(GerarOrcamentoInput.class))).thenReturn(orcamento);

        mockMvc.perform(post("/orcamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orcamento.getId()))
                .andExpect(jsonPath("$.ordemServicoId").value("os-100"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.valorTotal").value(45.00));
    }

    @Test
    @DisplayName("PATCH /orcamentos/{id}/enviar deve retornar 200 com orçamento ENVIADO")
    void deveEnviarOrcamentoERetornar200() throws Exception {
        var itemDomain = new ItemOrcamento("ins-1", null, "Filtro", 1, new BigDecimal("45.00"));
        var orcamento = Orcamento.criar("os-100", "cli-200", List.of(itemDomain), "Obs").enviar();

        when(enviarOrcamentoUseCase.executar(orcamento.getId())).thenReturn(orcamento);

        mockMvc.perform(patch("/orcamentos/{id}/enviar", orcamento.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENVIADO"));
    }

    @Test
    @DisplayName("PATCH /orcamentos/{id}/aprovar deve retornar 200 com orçamento APROVADO")
    void deveAprovarOrcamentoERetornar200() throws Exception {
        var itemDomain = new ItemOrcamento("ins-1", null, "Filtro", 1, new BigDecimal("45.00"));
        var orcamento = Orcamento.criar("os-100", "cli-200", List.of(itemDomain), "Obs").enviar().aprovar();

        when(aprovarOrcamentoUseCase.executar(orcamento.getId())).thenReturn(orcamento);

        mockMvc.perform(patch("/orcamentos/{id}/aprovar", orcamento.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    @DisplayName("GET /orcamentos/{id} deve retornar 200 com orçamento")
    void deveBuscarPorIdERetornar200() throws Exception {
        var itemDomain = new ItemOrcamento("ins-1", null, "Filtro", 1, new BigDecimal("45.00"));
        var orcamento = Orcamento.criar("os-100", "cli-200", List.of(itemDomain), "Obs");

        when(consultarOrcamentoUseCase.buscarPorId(orcamento.getId())).thenReturn(orcamento);

        mockMvc.perform(get("/orcamentos/{id}", orcamento.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orcamento.getId()))
                .andExpect(jsonPath("$.ordemServicoId").value("os-100"));
    }
}
