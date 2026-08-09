package com.systemdesign.orbit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * DataSource/JPA/Flyway autoconfiguration is excluded here UNCONDITIONALLY and re-imported only
 * by {@link com.systemdesign.orbit.config.PostgresPersistenceConfig}, which is itself gated
 * behind the "postgres" Spring profile. That's what makes {@code app.repository=memory} a
 * genuinely zero-database mode: no DataSource bean is ever created, no connection is ever
 * attempted — not "configured but unused," but never built at all.
 *
 * <p>The "postgres" profile is activated here, based on the same {@code APP_REPOSITORY} env var
 * that config.CoreBeansConfig reads to pick the SubscriptionRepositoryPort adapter, because
 * profile activation has to happen before the Spring context is built — a property placeholder
 * inside application.yml can't switch profiles on itself.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
public class OrbitApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(OrbitApplication.class);

        String repository = System.getenv().getOrDefault(
                "APP_REPOSITORY", System.getProperty("APP_REPOSITORY", "memory"));
        if ("postgres".equalsIgnoreCase(repository)) {
            app.setAdditionalProfiles("postgres");
        }

        app.run(args);
    }
}
