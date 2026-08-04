package com.systemdesign.ecommarketplace.wallet;

import com.systemdesign.ecommarketplace.common.exceptions.NotFoundException;
import com.systemdesign.ecommarketplace.sharding.ShardRouterService;
import com.systemdesign.ecommarketplace.wallet.entity.LedgerEntryType;
import com.systemdesign.ecommarketplace.wallet.entity.Wallet;
import com.systemdesign.ecommarketplace.wallet.entity.WalletLedgerEntry;
import com.systemdesign.ecommarketplace.wallet.repository.WalletLedgerEntryRepository;
import com.systemdesign.ecommarketplace.wallet.repository.WalletRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Mirrors src/modules/wallet/wallet.service.ts. */
@Service
public class WalletService {

  private static final Logger log = LoggerFactory.getLogger(WalletService.class);

  /** Demo "welcome bonus" so settlement debits have something to draw from. */
  public static final int SIGNUP_BONUS_CENTS = 500000;

  private final ShardRouterService shardRouter;

  public WalletService(ShardRouterService shardRouter) {
    this.shardRouter = shardRouter;
  }

  public Wallet getWallet(String userId) {
    WalletRepository repo = shardRouter.getRepository(WalletRepository.class, userId);
    return repo.findByUserId(UUID.fromString(userId))
        .orElseThrow(() -> new NotFoundException("Wallet for user " + userId + " not found"));
  }

  public List<WalletLedgerEntry> getLedger(String userId, int limit) {
    Wallet wallet = getWallet(userId);
    WalletLedgerEntryRepository ledgerRepo = shardRouter.getRepository(WalletLedgerEntryRepository.class, userId);
    return ledgerRepo.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), PageRequest.of(0, limit));
  }

  public Wallet credit(String userId, int amountCents, String reason, String referenceId) {
    return applyLedgerEntry(userId, LedgerEntryType.CREDIT, amountCents, reason, referenceId);
  }

  public Wallet debit(String userId, int amountCents, String reason, String referenceId) {
    return applyLedgerEntry(userId, LedgerEntryType.DEBIT, amountCents, reason, referenceId);
  }

  /**
   * Credit and debit both run inside a transaction scoped to the user's OWN
   * shard - this only works cleanly because Wallet is colocated with its
   * owning User (see Wallet entity doc comment). A cross-shard wallet
   * transfer would need a saga/2PC; a same-shard debit is just a normal
   * ACID transaction.
   */
  private Wallet applyLedgerEntry(
      String userId, LedgerEntryType type, int amountCents, String reason, String referenceId) {
    int shardIndex = shardRouter.resolveShardIndex(userId);
    TransactionTemplate tx = shardRouter.getTransactionTemplate(shardIndex);

    return tx.execute(
        status -> {
          WalletRepository walletRepo = shardRouter.getRepository(WalletRepository.class, shardIndex);
          WalletLedgerEntryRepository ledgerRepo =
              shardRouter.getRepository(WalletLedgerEntryRepository.class, shardIndex);

          Wallet wallet =
              walletRepo
                  .findByUserIdForUpdate(UUID.fromString(userId))
                  .orElseThrow(() -> new NotFoundException("Wallet for user " + userId + " not found"));

          int delta = type == LedgerEntryType.CREDIT ? amountCents : -amountCents;
          wallet.setBalanceCents(wallet.getBalanceCents() + delta);
          if (wallet.getBalanceCents() < 0) {
            log.warn(
                "Wallet {} balance went negative ({}) after {} of {} for \"{}\" - allowed in this demo,"
                    + " would be rejected/flagged in production",
                wallet.getId(),
                wallet.getBalanceCents(),
                type,
                amountCents,
                reason);
          }
          walletRepo.save(wallet);

          WalletLedgerEntry entry = new WalletLedgerEntry();
          entry.setId(UUID.randomUUID());
          entry.setWalletId(wallet.getId());
          entry.setType(type);
          entry.setAmountCents(amountCents);
          entry.setReason(reason);
          entry.setReferenceId(referenceId != null ? UUID.fromString(referenceId) : null);
          ledgerRepo.save(entry);

          return wallet;
        });
  }
}
