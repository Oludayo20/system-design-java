package com.systemdesign.ecommarketplace.infrastructure.postgres;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The PRIMARY database: Marketplace (Category/Product), Orders/OrderItem,
 * and the email-&gt;shard UserDirectory. Mirrors
 * src/infrastructure/postgres/primary.data-source.ts and the
 * `TypeOrmModule.forRootAsync({ name: 'primary', ... })` block in
 * app.module.ts.
 *
 * <p>Repositories for these entities are ordinary Spring Data JPA
 * repositories (unlike the shard entities), since there's exactly one
 * primary database - {@code @EnableJpaRepositories} below binds the
 * marketplace/orders/auth repository packages to this one
 * EntityManagerFactory/TransactionManager pair.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = {
      "com.systemdesign.ecommarketplace.marketplace.repository",
      "com.systemdesign.ecommarketplace.orders.repository",
      "com.systemdesign.ecommarketplace.auth.repository"
    },
    entityManagerFactoryRef = "primaryEntityManagerFactory",
    transactionManagerRef = "primaryTransactionManager")
public class PrimaryDataSourceConfig {

  @org.springframework.context.annotation.Primary
  @Bean(name = "primaryDataSource")
  public DataSource primaryDataSource(
      @Value("${app.datasource.primary.host}") String host,
      @Value("${app.datasource.primary.port}") int port,
      @Value("${app.datasource.primary.username}") String username,
      @Value("${app.datasource.primary.password}") String password,
      @Value("${app.datasource.primary.database}") String database) {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
    ds.setUsername(username);
    ds.setPassword(password);
    ds.setDriverClassName("org.postgresql.Driver");
    ds.setPoolName("primary-pool");
    return ds;
  }

  /**
   * Runs src/infrastructure/postgres/migrations/primary/*.ts's SQL (ported
   * 1:1 to db/migration/primary/*.sql) against the primary DB only, on
   * startup, before the EntityManagerFactory (and therefore Hibernate's
   * schema validation) is created - see @DependsOn below.
   */
  @Bean(name = "primaryFlyway", initMethod = "migrate")
  public Flyway primaryFlyway(@Qualifier("primaryDataSource") DataSource dataSource) {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration/primary")
        .baselineOnMigrate(true)
        .load();
  }

  @org.springframework.context.annotation.Primary
  @Bean(name = "primaryEntityManagerFactory")
  @DependsOn("primaryFlyway")
  public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
      @Qualifier("primaryDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
    emf.setDataSource(dataSource);
    emf.setPersistenceUnitName("primary");
    emf.setPackagesToScan(
        "com.systemdesign.ecommarketplace.marketplace.entity",
        "com.systemdesign.ecommarketplace.orders.entity",
        "com.systemdesign.ecommarketplace.auth.entity");
    JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    emf.setJpaVendorAdapter(vendorAdapter);
    Map<String, Object> props = new HashMap<>();
    // Schema is owned entirely by Flyway (ported migrations), matching the
    // original's `synchronize: false` - Hibernate only validates the
    // mapping against what Flyway already created.
    props.put("hibernate.hbm2ddl.auto", "validate");
    props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    emf.setJpaPropertyMap(props);
    return emf;
  }

  @org.springframework.context.annotation.Primary
  @Bean(name = "primaryTransactionManager")
  public PlatformTransactionManager primaryTransactionManager(
      @Qualifier("primaryEntityManagerFactory") jakarta.persistence.EntityManagerFactory emf) {
    return new JpaTransactionManager(emf);
  }

  /**
   * A transaction-aware shared EntityManager for services (e.g. OrdersService)
   * that need direct EntityManager access rather than a Spring Data
   * repository. Only usable inside a transaction bound to
   * primaryTransactionManager (see primaryTransactionTemplate).
   */
  @Bean(name = "primaryEntityManager")
  public EntityManager primaryEntityManager(
      @Qualifier("primaryEntityManagerFactory") jakarta.persistence.EntityManagerFactory emf) {
    return SharedEntityManagerCreator.createSharedEntityManager(emf);
  }

  /**
   * Mirrors `@InjectDataSource('primary') dataSource: DataSource` +
   * `dataSource.transaction(async manager => {...})` in orders.service.ts:
   * OrdersService executes its persist step inside
   * primaryTransactionTemplate.execute(...), then publishes order.created
   * AFTER the callback returns - i.e. after commit, not inside the
   * transaction.
   */
  @Bean(name = "primaryTransactionTemplate")
  public TransactionTemplate primaryTransactionTemplate(
      @Qualifier("primaryTransactionManager") PlatformTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }
}
