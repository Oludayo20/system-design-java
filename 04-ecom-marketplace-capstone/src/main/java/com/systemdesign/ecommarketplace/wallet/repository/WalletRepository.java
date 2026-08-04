package com.systemdesign.ecommarketplace.wallet.repository;

import com.systemdesign.ecommarketplace.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * NOT bound via @EnableJpaRepositories - built on demand by
 * ShardRouterService.getRepository() against the resolved shard's shared
 * EntityManager. See Shard0/1/2DataSourceConfig's class comment.
 */
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

  Optional<Wallet> findByUserId(UUID userId);

  /**
   * SELECT ... FOR UPDATE, mirroring TypeORM's
   * `manager.findOne(Wallet, { where: { userId }, lock: { mode:
   * 'pessimistic_write' } })` in WalletService.applyLedgerEntry - credit/debit
   * must serialize concurrent balance updates for the same wallet.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
  Optional<Wallet> findByUserIdForUpdate(@Param("userId") UUID userId);

  void deleteByUserId(UUID userId);
}
