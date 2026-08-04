package com.systemdesign.ecommarketplace.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

/**
 * Mirrors src/modules/wallet/entities/wallet.entity.ts. Colocated with its
 * owning User on the same shard database (both keyed by hash(userId) %
 * SHARD_COUNT). This is a deliberate sharding-best-practice decision, not
 * an accident: a wallet is meaningless without its user, and the two are
 * almost always read/written together (profile view shows balance; order
 * settlement debits the wallet for a specific user). Storing them on the
 * same physical Postgres instance means every wallet operation is a
 * single-shard, single-database transaction - no distributed
 * transaction/2PC across shards is ever needed for the user+wallet unit.
 *
 * <p>No JPA relation to User is declared (same as the original, which has
 * no TypeORM relation from Wallet to User either) - just a plain userId
 * column, with the FK enforced at the DB level by the shard migration.
 *
 * <p>Implements {@link Persistable} for the same reason as User/Order: the
 * id is assigned in application code (AuthService.register), so Spring Data
 * JPA needs the explicit isNew flag rather than its default "id == null"
 * check to correctly persist() on first save and merge()/update on
 * subsequent saves (WalletService.applyLedgerEntry loads-then-saves the
 * same Wallet inside every credit/debit).
 */
@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
public class Wallet implements Persistable<UUID> {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Transient
  private boolean isNew = true;

  @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "uuid")
  private UUID userId;

  @Column(name = "balance_cents", nullable = false)
  private int balanceCents;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PostLoad
  @PostPersist
  void markNotNew() {
    this.isNew = false;
  }
}
