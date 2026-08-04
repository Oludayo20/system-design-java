package com.systemdesign.ecommarketplace.sharding;

import com.systemdesign.ecommarketplace.sharding.strategies.HashShardingStrategy;
import jakarta.persistence.EntityManager;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Single choke point for "which physical database owns this user?". Mirrors
 * src/sharding/shard-router.service.ts.
 *
 * <p>Every module that touches User or Wallet data goes through this
 * service instead of injecting a shard DataSource/EntityManager directly.
 * That keeps the shard key (hash(userId) % SHARD_COUNT) defined in exactly
 * one place, and keeps every query in the app single-shard - nothing here
 * ever scatter-gathers across all three shards, which is deliberate: the
 * whole point of sharding by userId is that a request for one user only
 * ever needs one connection.
 *
 * <p>Repository access mirrors the original's {@code dataSource.getRepository
 * (Entity)}: {@link #getRepository(Class, int)} builds a live Spring Data
 * repository proxy on demand via {@link JpaRepositoryFactory}, bound to the
 * resolved shard's shared EntityManager, rather than a repository interface
 * being permanently wired (via {@code @EnableJpaRepositories}) to a single
 * EntityManagerFactory - that wiring model doesn't fit "the same
 * UserRepository must work against whichever of 3 databases a given userId
 * happens to hash to".
 */
@Service
public class ShardRouterService {

  private final HashShardingStrategy strategy;
  private final List<DataSource> dataSources;
  private final List<EntityManager> entityManagers;
  private final List<PlatformTransactionManager> transactionManagers;

  public ShardRouterService(
      @Value("${app.shard-count}") int shardCount,
      @Qualifier("shard0DataSource") DataSource shard0DataSource,
      @Qualifier("shard1DataSource") DataSource shard1DataSource,
      @Qualifier("shard2DataSource") DataSource shard2DataSource,
      @Qualifier("shard0EntityManager") EntityManager shard0EntityManager,
      @Qualifier("shard1EntityManager") EntityManager shard1EntityManager,
      @Qualifier("shard2EntityManager") EntityManager shard2EntityManager,
      @Qualifier("shard0TransactionManager") PlatformTransactionManager shard0TransactionManager,
      @Qualifier("shard1TransactionManager") PlatformTransactionManager shard1TransactionManager,
      @Qualifier("shard2TransactionManager") PlatformTransactionManager shard2TransactionManager) {
    this.strategy = new HashShardingStrategy(shardCount);
    this.dataSources = List.of(shard0DataSource, shard1DataSource, shard2DataSource);
    this.entityManagers = List.of(shard0EntityManager, shard1EntityManager, shard2EntityManager);
    this.transactionManagers =
        List.of(shard0TransactionManager, shard1TransactionManager, shard2TransactionManager);
  }

  public int resolveShardIndex(String userId) {
    return strategy.resolveShard(userId);
  }

  public DataSource getDataSource(int shardIndex) {
    validateIndex(shardIndex);
    return dataSources.get(shardIndex);
  }

  public DataSource getDataSourceForUser(String userId) {
    return getDataSource(resolveShardIndex(userId));
  }

  /**
   * Builds a live Spring Data repository for {@code repositoryInterface},
   * bound to the shard that owns {@code userId}. Equivalent to the
   * original's {@code shardRouter.getRepository(User, userId)}.
   */
  public <R> R getRepository(Class<R> repositoryInterface, String userId) {
    return getRepository(repositoryInterface, resolveShardIndex(userId));
  }

  /** Same as above, but for a shard index already resolved by the caller. */
  public <R> R getRepository(Class<R> repositoryInterface, int shardIndex) {
    validateIndex(shardIndex);
    EntityManager entityManager = entityManagers.get(shardIndex);
    JpaRepositoryFactory factory = new JpaRepositoryFactory(entityManager);
    return factory.getRepository(repositoryInterface);
  }

  /**
   * A TransactionTemplate scoped to one shard's PlatformTransactionManager.
   * Mirrors {@code shardDataSource.transaction(async manager => {...})} in
   * the original AuthService/WalletService: wrap the shard repository calls
   * made inside the callback in {@code template.execute(status -> {...})}.
   */
  public TransactionTemplate getTransactionTemplate(int shardIndex) {
    validateIndex(shardIndex);
    return new TransactionTemplate(transactionManagers.get(shardIndex));
  }

  public TransactionTemplate getTransactionTemplateForUser(String userId) {
    return getTransactionTemplate(resolveShardIndex(userId));
  }

  public List<DataSource> getAllDataSources() {
    return dataSources;
  }

  private void validateIndex(int shardIndex) {
    if (shardIndex < 0 || shardIndex >= dataSources.size()) {
      throw new IllegalStateException("No DataSource registered for shard index " + shardIndex);
    }
  }
}
