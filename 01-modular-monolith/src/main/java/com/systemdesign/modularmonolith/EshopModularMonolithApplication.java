package com.systemdesign.modularmonolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single deployable, single process: the entire E-Shop application -- Identity, Catalog, Basket,
 * Ordering, Inventory, Notifications -- boots from this one entry point, mirroring
 * {@code src/main.ts}. Modules stay decoupled from each other through {@code EventBus} /
 * RabbitMQ, not through separate deployments.
 */
@SpringBootApplication
public class EshopModularMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(EshopModularMonolithApplication.class, args);
    }
}
