package com.mecanica.oficina_billing_service.adapters.web.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MercadoPagoWebhookRequest {

    private String id;
    private String action;
    private String type;

    @JsonProperty("live_mode")
    private Boolean liveMode;

    private DataPayload data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataPayload {
        private String id;
    }
}
