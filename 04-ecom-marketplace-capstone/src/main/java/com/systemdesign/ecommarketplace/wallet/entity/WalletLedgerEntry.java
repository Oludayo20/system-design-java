package com.systemdesign.ecommarketplace.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * Mirrors src/modules/wallet/entities/wallet-ledger-entry.entity.ts.
 * Append-only ledger - Wallet.balanceCents is a cached sum, this is the
 * audit trail. Lives on a shard database, colocated with its Wallet/User.
 *
 * <p>`type` maps to the native Postgres enum `ledger_entry_type_enum`
 * created by the shard migration (see db/migration/shard) via Hibernate 6's
 * {@code @JdbcTypeCode(SqlTypes.NAMED_ENUM)}, matching the original's
 * TypeORM `type: 'enum'` column exactly rather than falling back to a plain
 * varchar.
 *
 * <p>Implements {@link Persistable} for the same app-assigned-id reason as
 * User/Wallet/Order - every ledger entry's id is set via
 * {@code UUID.randomUUID()} in WalletService before save(), so Spring Data
 * needs the explicit isNew flag rather than its default null-id check.
 */
@Entity
@Table(name = "wallet_ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class WalletLedgerEntry implements Persistable<UUID> {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Transient
  private boolean isNew = true;

  @Column(name = "wallet_id", nullable = false, columnDefinition = "uuid")
  private UUID walletId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "type", nullable = false, columnDefinition = "ledger_entry_type_enum")
  private LedgerEntryType type;

  @Column(name = "amount_cents", nullable = false)
  private int amountCents;

  @Column(nullable = false)
  private String reason;

  // e.g. the orderId that triggered a settlement debit.
  @Column(name = "reference_id", columnDefinition = "uuid")
  private UUID referenceId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PostLoad
  @PostPersist
  void markNotNew() {
    this.isNew = false;
  }
}
