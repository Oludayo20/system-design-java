package com.systemdesign.ecommarketplace.auth.entity;

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
import org.springframework.data.domain.Persistable;

/**
 * Mirrors src/modules/auth/entities/user-directory.entity.ts. Lives on the
 * PRIMARY (unsharded) database and answers exactly one question at login
 * time: "given an email, which shard is this user's row on?".
 *
 * <p>Why not just hash the email? Because the shard key is hash(userId), not
 * hash(email) - a user's userId is generated once at registration and never
 * changes, whereas allowing email changes to silently move a user's data to
 * a different shard would be a much harder problem. So something has to
 * remember the email -> {userId, shardIndex} mapping, and that something
 * can't live *on* a shard (you'd have to guess the shard to find it, which
 * is the chicken-and-egg problem sharding creates for any lookup key that
 * isn't the shard key itself).
 *
 * <p>Tradeoff (documented in README): this table is a second write on every
 * registration, in a *different* database than the shard the user row was
 * just written to. There is no distributed transaction across the two -
 * AuthService writes the shard row first, then this directory row, and
 * compensates (deletes the shard row) if the second write fails. That
 * leaves a narrow window where a crash between the two writes could strand
 * an orphaned user row on a shard with no directory entry pointing at it.
 * In production this would be closed with an outbox/reconciliation job; for
 * this demo the compensating delete is judged sufficient.
 *
 * <p>Implements {@link Persistable} for the same app-assigned-id reason as
 * the shard entities - AuthService.register assigns this row's id via
 * {@code UUID.randomUUID()} before save(), so Spring Data needs the
 * explicit isNew flag rather than its default null-id check to correctly
 * persist() (rather than an unnecessary merge()) on first save.
 */
@Entity
@Table(name = "user_directory")
@Getter
@Setter
@NoArgsConstructor
public class UserDirectory implements Persistable<UUID> {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Transient
  private boolean isNew = true;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "uuid")
  private UUID userId;

  @Column(name = "shard_index", nullable = false)
  private int shardIndex;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PostLoad
  @PostPersist
  void markNotNew() {
    this.isNew = false;
  }
}
