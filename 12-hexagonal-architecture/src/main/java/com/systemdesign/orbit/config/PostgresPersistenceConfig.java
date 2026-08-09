package com.systemdesign.orbit.config;

import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Re-enables the DataSource/JPA/Flyway autoconfiguration that {@link
 * com.systemdesign.orbit.OrbitApplication} excludes by default — but only when the "postgres"
 * profile is active (i.e. {@code APP_REPOSITORY=postgres}). This is the one place in the whole
 * project where a database connection is ever attempted.
 */
@Configuration
@Profile("postgres")
@Import({
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
@EnableJpaRepositories(basePackages = "com.systemdesign.orbit.adapters.out.persistence")
@EntityScan(basePackages = "com.systemdesign.orbit.adapters.out.persistence")
public class PostgresPersistenceConfig {
}
