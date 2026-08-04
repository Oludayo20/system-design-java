package com.systemdesign.ecommarketplace.wallet.repository;

import com.systemdesign.ecommarketplace.wallet.entity.WalletLedgerEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** NOT bound via @EnableJpaRepositories - see WalletRepository's class comment. */
public interface WalletLedgerEntryRepository extends JpaRepository<WalletLedgerEntry, UUID> {
  List<WalletLedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);
}
