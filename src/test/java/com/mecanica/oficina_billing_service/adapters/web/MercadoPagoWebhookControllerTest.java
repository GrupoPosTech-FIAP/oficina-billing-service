package com.mecanica.oficina_billing_service.adapters.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanica.oficina_billing_service.adapters.mercadopago.MercadoPagoWebhookValidator;
import com.mecanica.oficina_billing_service.adapters.web.dto.request.MercadoPagoWebhookRequest;
import com.mecanica.oficina_billing_service.application.cobranca.exception.InvalidWebhookSignatureException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ProcessarWebhookMercadoPagoUseCase processarWebhookUseCase;

    @Mock
    private MercadoPagoWebhookValidator webhookValidator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        MercadoPagoWebhookController controller = new MercadoPagoWebhookController(processarWebhookUseCase, webhookValidator, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /webhooks/mercadopago com JSON e assinatura válida deve processar e retornar 200 ok")
    void deveProcessarWebhookComJsonEAssinaturaValida() throws Exception {
        var request = new MercadoPagoWebhookRequest("123", "payment.updated", "payment", true,
                new MercadoPagoWebhookRequest.DataPayload("99887766"));

        mockMvc.perform(post("/webhooks/mercadopago")
                        .header("x-signature", "ts=1700000000,v1=validhash")
                        .header("x-request-id", "req-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        verify(webhookValidator).validarAssinatura(eq("ts=1700000000,v1=validhash"), eq("req-123"), eq("99887766"));
        verify(processarWebhookUseCase).executar(any(ProcessarWebhookInput.class));
    }

    @Test
    @DisplayName("POST /webhooks/mercadopago com assinatura inválida deve retornar 401 Unauthorized")
    void deveRetornar401QuandoAssinaturaInvalida() throws Exception {
        var request = new MercadoPagoWebhookRequest("123", "payment.updated", "payment", true,
                new MercadoPagoWebhookRequest.DataPayload("99887766"));

        doThrow(new InvalidWebhookSignatureException("Assinatura HMAC inválida"))
                .when(webhookValidator).validarAssinatura(any(), any(), any());

        mockMvc.perform(post("/webhooks/mercadopago")
                        .header("x-signature", "ts=1700000000,v1=badhash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(processarWebhookUseCase);
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
