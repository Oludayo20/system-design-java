package com.systemdesign.faas.triggers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.systemdesign.faas.runtime.ExecutionEnvironmentManager;
import com.systemdesign.faas.runtime.InvokeResult;
import com.systemdesign.faas.runtime.LambdaEvent;

/**
 * HTTP trigger, named after Amazon API Gateway to make the mapping explicit: in production this
 * front door would be API Gateway routing {@code POST /orders} to a Lambda; here it is a Spring
 * controller calling into the SAME {@link ExecutionEnvironmentManager} every other trigger uses.
 */
@Tag(name = "apiGateway trigger", description = "HTTP trigger -> createOrder")
@RestController
public class ApiGatewayController {

    private final ExecutionEnvironmentManager manager;

    public ApiGatewayController(ExecutionEnvironmentManager manager) {
        this.manager = manager;
    }

    @Operation(
            summary = "Create an order (HTTP-triggered Lambda invocation)",
            description = "Proves cold vs. warm via the X-Cold-Start / X-Billed-Duration-Ms response headers and the "
                    + "runtime field in the JSON body. Call twice back to back to see the second call go warm.")
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody(required = false) Map<String, Object> body) {
        InvokeResult result = manager.invoke("createOrder", LambdaEvent.of(body == null ? Map.of() : body));
        return TriggerResponses.toResponseEntity(result);
    }
}
