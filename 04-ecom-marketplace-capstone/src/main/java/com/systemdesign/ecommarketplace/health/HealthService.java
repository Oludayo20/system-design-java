package com.systemdesign.ecommarketplace.health;

import com.systemdesign.ecommarketplace.infrastructure.redis.RedisService;
import com.systemdesign.ecommarketplace.sharding.ShardRouterService;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Mirrors src/modules/health/health.service.ts. Backs GET /health - what
 * Nginx/compose healthchecks poll. Checks every piece of infra a request
 * might touch: the primary DB, all three shards (a request that needs a
 * specific user is only ever ONE of these, but the healthcheck itself
 * verifies all three are reachable), Redis, and RabbitMQ.
 */
@Service
public class HealthService {

  private static final Logger log = LoggerFactory.getLogger(HealthService.class);
  private static final int PROBE_TIMEOUT_SECONDS = 2;

  private final DataSource primaryDataSource;
  private final ShardRouterService shardRouter;
  private final RedisService redisService;
  private final ConnectionFactory rabbitConnectionFactory;

  public HealthService(
      @Qualifier("primaryDataSource") DataSource primaryDataSource,
      ShardRouterService shardRouter,
      RedisService redisService,
      ConnectionFactory rabbitConnectionFactory) {
    this.primaryDataSource = primaryDataSource;
    this.shardRouter = shardRouter;
    this.redisService = redisService;
    this.rabbitConnectionFactory = rabbitConnectionFactory;
  }

  public HealthReport check(String instanceId) {
    Map<String, String> services = new LinkedHashMap<>();

    services.put("postgres-primary", probe(() -> probeDataSource(primaryDataSource)));

    List<DataSource> shardDataSources = shardRouter.getAllDataSources();
    for (int i = 0; i < shardDataSources.size(); i++) {
      DataSource ds = shardDataSources.get(i);
      services.put("postgres-shard-" + i, probe(() -> probeDataSource(ds)));
    }

    services.put("redis", probe(redisService::ping));
    services.put("rabbitmq", probe(this::probeRabbitMq));

    boolean allUp = services.values().stream().allMatch("up"::equals);
    return new HealthReport(allUp ? "ok" : "degraded", instanceId, services);
  }

  private String probe(Callable<?> fn) {
    try {
      fn.call();
      return "up";
    } catch (Exception e) {
      log.warn("Health probe failed: {}", e.getMessage());
      return "down";
    }
  }

  private Boolean probeDataSource(DataSource dataSource) throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      return connection.isValid(PROBE_TIMEOUT_SECONDS);
    }
  }

  private Boolean probeRabbitMq() {
    var connection = rabbitConnectionFactory.createConnection();
    try {
      return connection.isOpen();
    } finally {
      connection.close();
    }
  }
}
