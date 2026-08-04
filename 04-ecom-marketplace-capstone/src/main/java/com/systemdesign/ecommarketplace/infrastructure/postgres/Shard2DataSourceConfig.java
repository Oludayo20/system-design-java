package com.systemdesign.ecommarketplace.infrastructure.postgres;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Shard 2 of 3: Users + Wallets, sharded by hash(userId) % SHARD_COUNT.
 * Mirrors src/infrastructure/postgres/shard2.data-source.ts.
 *
 * <p>Deliberately NOT annotated with {@code @EnableJpaRepositories}: the
 * exact same UserRepository/WalletRepository/WalletLedgerEntryRepository
 * interfaces must be usable against whichever shard a given userId
 * resolves to, not bound permanently to one EntityManagerFactory. Instead,
 * ShardRouterService builds live repository instances on demand via
 * {@code JpaRepositoryFactory}, against this shard's shared EntityManager -
 * see ShardRouterService.getRepository(). This class only wires the
 * plumbing (DataSource/Flyway/EntityManagerFactory/TransactionManager) that
 * ShardRouterService consumes.
 */
@Configuration
public class Shard2DataSourceConfig {

  @Bean(name = "shard2DataSource")
  public DataSource shard2DataSource(
      @Value("${app.datasource.shard2.host}") String host,
      @Value("${app.datasource.shard2.port}") int port,
      @Value("${app.datasource.shard2.username}") String username,
      @Value("${app.datasource.shard2.password}") String password,
      @Value("${app.datasource.shard2.database}") String database) {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
    ds.setUsername(username);
    ds.setPassword(password);
    ds.setDriverClassName("org.postgresql.Driver");
    ds.setPoolName("shard2-pool");
    return ds;
  }

  /**
   * Runs the SAME shard schema migration as shard1/shard2 (each shard keeps
   * its own flyway_schema_history table - see db/migration/shard). This is
   * expected, not a bug: identical migration file, three independent
   * databases.
   */
  @Bean(name = "shard2Flyway", initMethod = "migrate")
  public Flyway shard2Flyway(@Qualifier("shard2DataSource") DataSource dataSource) {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration/shard")
        .baselineOnMigrate(true)
        .load();
  }

  @Bean(name = "shard2EntityManagerFactory")
  @DependsOn("shard2Flyway")
  public LocalContainerEntityManagerFactoryBean shard2EntityManagerFactory(
      @Qualifier("shard2DataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
    emf.setDataSource(dataSource);
    emf.setPersistenceUnitName("shard2");
    emf.setPackagesToScan(
        "com.systemdesign.ecommarketplace.users.entity", "com.systemdesign.ecommarketplace.wallet.entity");
    JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    emf.setJpaVendorAdapter(vendorAdapter);
    Map<String, Object> props = new HashMap<>();
    props.put("hibernate.hbm2ddl.auto", "validate");
    props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    emf.setJpaPropertyMap(props);
    return emf;
  }

  @Bean(name = "shard2TransactionManager")
  public PlatformTransactionManager shard2TransactionManager(
      @Qualifier("shard2EntityManagerFactory") EntityManagerFactory emf) {
    return new JpaTransactionManager(emf);
  }

  @Bean(name = "shard2EntityManager")
  public EntityManager shard2EntityManager(@Qualifier("shard2EntityManagerFactory") EntityManagerFactory emf) {
    return SharedEntityManagerCreator.createSharedEntityManager(emf);
  }

  @Bean(name = "shard2TransactionTemplate")
  public TransactionTemplate shard2TransactionTemplate(
      @Qualifier("shard2TransactionManager") PlatformTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }
}
