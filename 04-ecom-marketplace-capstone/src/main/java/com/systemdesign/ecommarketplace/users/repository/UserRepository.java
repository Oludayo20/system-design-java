package com.systemdesign.ecommarketplace.users.repository;

import com.systemdesign.ecommarketplace.users.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * NOT bound via @EnableJpaRepositories - identical to
 * WalletRepository/WalletLedgerEntryRepository, this interface is turned
 * into a live repository proxy on demand by
 * ShardRouterService.getRepository(), against whichever shard's shared
 * EntityManager owns a given userId. See Shard0/1/2DataSourceConfig's class
 * comment for why.
 */
public interface UserRepository extends JpaRepository<User, UUID> {}
