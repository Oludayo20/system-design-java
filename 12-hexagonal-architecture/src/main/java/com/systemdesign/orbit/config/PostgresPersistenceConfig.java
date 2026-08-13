package com.systemdesign.orbit.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Re-enables the DataSource/JPA/Flyway autoconfiguration that {@link
 * com.systemdesign.orbit.OrbitApplication} excludes by default — but only when the "postgres"
 * profile is active (i.e. {@code APP_REPOSITORY=postgres}). This is the one place in the whole
 * project where a database connection is ever attempted.
 *
 * <p>Deliberately does NOT import Boot's {@code JpaRepositoriesAutoConfiguration}: that
 * autoconfiguration auto-detects repository base packages via {@code AutoConfigurationPackages},
 * a registration that only happens as part of the deferred auto-configuration import phase — and
 * since this class reaches {@code JpaRepositoriesAutoConfiguration} through a plain {@code
 * @Import} (processed earlier, non-deferred), that registration isn't guaranteed to have run yet.
 * {@link EnableJpaRepositories} below is the plain Spring Data mechanism instead: given an
 * explicit {@code basePackages}, it never needs {@code AutoConfigurationPackages} at all.
 *
 * <p>Also deliberately does NOT import {@code DataSourceTransactionManagerAutoConfiguration}
 * (the plain-JDBC transaction manager): {@code HibernateJpaAutoConfiguration} already registers
 * its own {@code JpaTransactionManager}, and both beans satisfy the same {@code
 * @ConditionalOnMissingBean(TransactionManager.class)} guard — whichever gets processed first
 * wins. In Boot's normal (deferred) autoconfiguration pipeline, ordering metadata guarantees JPA's
 * manager wins; a plain {@code @Import} array has no such guarantee, and having listed the JDBC
 * one first here originally, IT won, silently registering as the active {@code
 * PlatformTransactionManager} — which does not know about Hibernate's {@code Session}, so every
 * {@code @Transactional} JPA repository call ran with no real transaction: writes were enlisted
 * in the persistence context but dropped, unflushed, when the EntityManager closed.
 *
 * <p>{@code @EnableTransactionManagement} is added explicitly (rather than relying on Boot's
 * {@code TransactionAutoConfiguration}, which itself is {@code @ConditionalOnBean(TransactionManager.class)})
 * for the same "manual @Import, no ordering guarantee" reason as above.
 */
@Configuration
@Profile("postgres")
@EnableTransactionManagement
@Import({
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
@EnableJpaRepositories(basePackages = "com.systemdesign.orbit.adapters.out.persistence")
@EntityScan(basePackages = "com.systemdesign.orbit.adapters.out.persistence")
public class PostgresPersistenceConfig {
}
