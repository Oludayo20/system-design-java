package com.systemdesign.bookhive.gateway.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.http.HttpClient;
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
     *
     * <p>Backed by {@link JdkClientHttpRequestFactory} (java.net.http.HttpClient) rather than
     * Spring's default {@code SimpleClientHttpRequestFactory} (JDK {@code HttpURLConnection}):
     * the latter throws {@code "cannot retry due to server authentication, in streaming mode"}
     * when relaying a request with a body to an endpoint that answers 401, because
     * {@code HttpURLConnection} tries to transparently retry the request for its own
     * auto-authentication handling and can't replay an already-streamed body. A reverse proxy
     * must be able to relay a plain 401 from a downstream service untouched, so this project
     * uses the newer client that doesn't have that behavior.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        RestTemplate restTemplate = builder.requestFactory(() -> requestFactory).build();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
        return restTemplate;
    }
}
