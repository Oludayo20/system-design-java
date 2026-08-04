package com.systemdesign.ecommarketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap. Mirrors src/main.ts: bootstrap the app, log
 * "[instanceId] Oja Marketplace Capstone listening on port ..." once ready.
 *
 * DataSource/Hibernate/Spring-Data-JPA-repositories/Flyway autoconfiguration
 * are excluded here (and in application.yml, belt-and-suspenders) because
 * this app wires FOUR DataSources by hand - one primary + three shards -
 * each with its own EntityManagerFactory, PlatformTransactionManager and
 * Flyway instance. See infrastructure.postgres.*Config and
 * sharding.ShardRouterService for the multi-datasource routing logic.
 */
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        FlywayAutoConfiguration.class
    })
public class EcomMarketplaceCapstoneApplication {

  private static final Logger log = LoggerFactory.getLogger(EcomMarketplaceCapstoneApplication.class);

  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(EcomMarketplaceCapstoneApplication.class, args);
    Environment env = context.getEnvironment();
    String port = env.getProperty("server.port", "3000");
    String instanceId = env.getProperty("app.instance-id", "local");
    log.info("[{}] Oja Marketplace Capstone listening on port {} (docs at /docs)", instanceId, port);
  }
}
