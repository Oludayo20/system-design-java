package com.systemdesign.bookhive.gateway.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class GatewayConfig {

    /**
     * A RestTemplate that never throws on a non-2xx response: every downstream status code
     * (including 4xx/5xx) is returned to the proxy as a normal {@code ResponseEntity} and
     * forwarded to the client as-is. This is what a reverse proxy is supposed to do - relay the
     * backend's response, not reinterpret it as a Java exception. Only genuine transport
     * failures (connection refused, DNS failure, timeout) still throw, which the proxy
     * translates into a 502.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
        return restTemplate;
    }
}
