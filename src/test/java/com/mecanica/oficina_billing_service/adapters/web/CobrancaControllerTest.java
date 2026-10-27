package com.mecanica.oficina_billing_service.adapters.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mecanica.oficina_billing_service.adapters.web.dto.request.CriarCobrancaRequest;
import com.mecanica.oficina_billing_service.application.cobranca.gateway.MercadoPagoGateway;
import com.mecanica.oficina_billing_service.application.cobranca.input.CriarCobrancaInput;
import com.mecanica.oficina_billing_service.application.cobranca.output.CobrancaOutput;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ConsultarCobrancaUseCase;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.CriarCobrancaUseCase;
import com.mecanica.oficina_billing_service.domain.cobranca.CobrancaStatus;
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
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CobrancaControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CriarCobrancaUseCase criarCobrancaUseCase;

    @Mock
    private ConsultarCobrancaUseCase consultarCobrancaUseCase;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        CobrancaController controller = new CobrancaController(criarCobrancaUseCase, consultarCobrancaUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /cobrancas deve retornar 201 com link de pagamento direto para Checkout Pro")
    void deveCriarCobrancaERetornar201() throws Exception {
        var request = new CriarCobrancaRequest(
                "os-123",
                "Troca Pastilhas",
                new BigDecimal("120.00"),
                "cliente@email.com",
                "Carlos",
                "12345678900",
                "https://oficina.com.br/retorno"
        );
        UUID id = UUID.randomUUID();

        var output = new CobrancaOutput(
                id,
                "os-123",
                new BigDecimal("120.00"),
                "Troca Pastilhas",
                "oficina-billing:dev:os-123",
                "pref-001",
                "https://mp.com/init",
                CobrancaStatus.PENDENTE,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(criarCobrancaUseCase.executar(any(CriarCobrancaInput.class))).thenReturn(output);

        mockMvc.perform(post("/cobrancas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cobrancaId").value(id.toString()))
                .andExpect(jsonPath("$.pedidoId").value("os-123"))
                .andExpect(jsonPath("$.valor").value(120.00))
                .andExpect(jsonPath("$.linkPagamento").value("https://mp.com/init"))
                .andExpect(jsonPath("$.referencia").value("oficina-billing:dev:os-123"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("POST /cobrancas com valor negativo deve retornar 400 Bad Request")
    void deveRetornar400QuandoValorInvalido() throws Exception {
        var request = new CriarCobrancaRequest("os-123", "Troca Pastilhas", new BigDecimal("-10.00"));

        mockMvc.perform(post("/cobrancas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Dados de entrada inválidos"));
    }

    @Test
    @DisplayName("GET /cobrancas/{id} deve retornar 200 com os dados da cobrança")
    void deveBuscarPorIdERetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        var output = new CobrancaOutput(
                id,
                "os-123",
                new BigDecimal("120.00"),
                "Troca Pastilhas",
                "ref-1",
                "pref-1",
                "https://mp.com/init",
                CobrancaStatus.PENDENTE,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(consultarCobrancaUseCase.buscarPorId(id)).thenReturn(output);

        mockMvc.perform(get("/cobrancas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.pedidoId").value("os-123"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }
}
