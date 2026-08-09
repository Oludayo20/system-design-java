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
 * File-drop trigger, simulating an S3 {@code ObjectCreated} event notification invoking
 * {@code resizeProductImage}. There is no real S3 bucket to watch locally, so this endpoint
 * exists purely as an honest stand-in for "S3 invoked Lambda" — in production, an actual S3 event
 * notification would call the same function with the same event shape, no code changes needed.
 */
@Tag(name = "file-drop trigger", description = "Simulated S3 ObjectCreated event -> resizeProductImage")
@RestController
public class FileDropController {

    private final ExecutionEnvironmentManager manager;

    public FileDropController(ExecutionEnvironmentManager manager) {
        this.manager = manager;
    }

    @Operation(
            summary = "Simulate an S3 upload event (no real S3 locally)",
            description = "Stands in for an S3 ObjectCreated notification invoking resizeProductImage.")
    @PostMapping("/_simulate/s3-upload")
    public ResponseEntity<Map<String, Object>> simulateUpload(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> safeBody = body == null ? Map.of() : body;
        Object key = safeBody.get("key");
        if (key == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "key is required (e.g. \"product-42/original.jpg\")"));
        }
        Object bucket = safeBody.getOrDefault("bucket", "oja-product-images");

        InvokeResult result = manager.invoke("resizeProductImage",
                LambdaEvent.of(Map.of("bucket", bucket, "key", key)));
        return TriggerResponses.toResponseEntity(result);
    }
}
