package com.systemdesign.asyncqueue.worker;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Consumer process: no HTTP server, no controllers, no database. Direct port of
 * src/worker.main.ts + src/worker.module.ts — the standalone Nest application context (created
 * via {@code NestFactory.createApplicationContext}, not {@code create}) becomes, here, a Spring
 * context started with {@link WebApplicationType#NONE} so no embedded Tomcat is ever bound.
 *
 * <p>This is what {@code docker compose up --scale worker=N} runs N copies of; each replica
 * independently connects to RabbitMQ and competes for messages on
 * email.queue/analytics.queue/loyalty.queue.
 *
 * <p>Two things keep this process's dependency graph free of Postgres/JPA, mirroring the
 * original's {@code WorkerModule} never importing {@code TypeOrmModule}:
 * <ul>
 *   <li>{@code scanBasePackages} excludes {@code com.systemdesign.asyncqueue.rides}, so the
 *       {@code Ride} entity / {@code RideRepository} are never picked up as beans.</li>
 *   <li>The {@code DataSource}/Hibernate/Flyway autoconfigurations are explicitly excluded below,
 *       so Spring Boot never tries to open a JDBC connection using the (here, absent)
 *       {@code POSTGRES_*} env vars — see this env var list in docker-compose.yml's
 *       {@code worker} service, which sets only {@code RABBITMQ_URL} and
 *       {@code EMAIL_FAILURE_RATE}, same as the original.</li>
 * </ul>
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.systemdesign.asyncqueue.workers",
                "com.systemdesign.asyncqueue.rabbitmq",
                "com.systemdesign.asyncqueue.config"
        },
        exclude = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                FlywayAutoConfiguration.class
        }
)
public class WorkerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(WorkerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
