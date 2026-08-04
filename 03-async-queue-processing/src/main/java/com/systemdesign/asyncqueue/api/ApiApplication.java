package com.systemdesign.asyncqueue.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Producer process: HTTP API only. Direct port of src/main.ts + src/app.module.ts.
 *
 * <p>Component-scan is deliberately restricted to the rides, rabbitmq and config packages —
 * {@code com.systemdesign.asyncqueue.workers} is never scanned by this application, exactly like
 * the original's {@code AppModule} never importing {@code WorkersModule}. This is the
 * "component-scan scoping" half of the API/worker process split described in the project README.
 *
 * <p>{@code @EntityScan}/{@code @EnableJpaRepositories} are needed explicitly because Spring Boot
 * otherwise resolves JPA entity/repository base packages from the {@code @SpringBootApplication}
 * class's OWN package ({@code com.systemdesign.asyncqueue.api}), not from {@code scanBasePackages}
 * — without these, {@code Ride}/{@code RideRepository} (which live under
 * {@code com.systemdesign.asyncqueue.rides}) would never be picked up.
 *
 * <p>This is the default entrypoint of the packaged jar (see pom.xml's spring-boot-maven-plugin
 * {@code mainClass}); {@link com.systemdesign.asyncqueue.worker.WorkerApplication} is the other
 * one, selected at runtime via the {@code LOADER_MAIN} env var (PropertiesLauncher) — see
 * docker-compose.yml's {@code worker} service.
 */
@SpringBootApplication(scanBasePackages = {
        "com.systemdesign.asyncqueue.rides",
        "com.systemdesign.asyncqueue.rabbitmq",
        "com.systemdesign.asyncqueue.config"
})
@EntityScan(basePackages = "com.systemdesign.asyncqueue.rides")
@EnableJpaRepositories(basePackages = "com.systemdesign.asyncqueue.rides")
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
