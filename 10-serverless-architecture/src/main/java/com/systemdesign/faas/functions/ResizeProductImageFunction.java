package com.systemdesign.faas.functions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systemdesign.faas.runtime.LambdaContext;
import com.systemdesign.faas.runtime.LambdaEvent;
import com.systemdesign.faas.runtime.LambdaFunction;
import com.systemdesign.faas.runtime.LambdaResponse;

/**
 * Simulates an S3 {@code ObjectCreated} -> Lambda trigger: takes {@code {bucket, key}} (the shape
 * S3 event notifications actually carry, simplified) and "resizes" the uploaded object. There is
 * no real S3 bucket or image library here — see {@code FileDropController} for how this gets
 * invoked without one.
 */
public class ResizeProductImageFunction implements LambdaFunction {

    private static final Logger log = LoggerFactory.getLogger(ResizeProductImageFunction.class);

    private final List<String> supportedVariants;

    public ResizeProductImageFunction() {
        ColdInit.simulateWork(30);
        this.supportedVariants = List.of("thumbnail", "medium");
        log.info("[resizeProductImage] cold init: image pipeline instance constructed");
    }

    @Override
    public LambdaResponse handle(LambdaEvent event, LambdaContext context) {
        // Simulated CPU-bound resize work (thumbnail + medium variants).
        long workMs = 150 + ThreadLocalRandom.current().nextLong(150);
        ColdInit.simulateWork(workMs);

        String bucket = String.valueOf(event.get("bucket"));
        String key = String.valueOf(event.get("key"));
        String outputKey = key.replaceFirst("(\\.[^./]+)$", "-resized$1");

        Map<String, Object> result = Map.of(
                "bucket", bucket,
                "inputKey", key,
                "outputKey", outputKey,
                "variantsGenerated", supportedVariants);

        return new LambdaResponse(200, result);
    }
}
