package com.systemdesign.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single deployable, single process: Riverside Library boots from this one entry point,
 * mirroring {@code src/main.ts} in the NestJS original. There is no HTTP call, message queue, or
 * network hop between layers here -- Presentation, Application, Domain, and Data Access are all
 * plain Java classes calling each other in-process. The layering is a compile-time discipline
 * (package structure, port interfaces, one-directional imports), not a runtime/deployment split.
 */
@SpringBootApplication
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
