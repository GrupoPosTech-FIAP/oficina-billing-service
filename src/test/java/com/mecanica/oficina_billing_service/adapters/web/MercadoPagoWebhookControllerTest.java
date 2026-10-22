package com.mecanica.oficina_billing_service.adapters.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanica.oficina_billing_service.adapters.web.dto.request.MercadoPagoWebhookRequest;
import com.mecanica.oficina_billing_service.application.cobranca.input.ProcessarWebhookInput;
import com.mecanica.oficina_billing_service.application.cobranca.usecase.ProcessarWebhookMercadoPagoUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ProcessarWebhookMercadoPagoUseCase processarWebhookUseCase;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        MercadoPagoWebhookController controller = new MercadoPagoWebhookController(processarWebhookUseCase, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /webhooks/mercadopago com JSON deve processar e retornar 200 ok")
    void deveProcessarWebhookComJson() throws Exception {
        var request = new MercadoPagoWebhookRequest("123", "payment.updated", "payment", true,
                new MercadoPagoWebhookRequest.DataPayload("99887766"));

        mockMvc.perform(post("/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        verify(processarWebhookUseCase).executar(any(ProcessarWebhookInput.class));
    }

    @Test
    @DisplayName("POST /webhooks/mercadopago com Query Params deve processar e retornar 200 ok")
    void deveProcessarWebhookComQueryParams() throws Exception {
        mockMvc.perform(post("/webhooks/mercadopago")
                        .param("id", "99887766")
                        .param("topic", "payment"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        verify(processarWebhookUseCase).executar(any(ProcessarWebhookInput.class));
    }
}
