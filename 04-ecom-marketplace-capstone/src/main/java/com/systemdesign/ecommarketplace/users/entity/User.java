package com.systemdesign.ecommarketplace.users.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * Mirrors src/modules/users/entities/user.entity.ts. Lives on a shard
 * database (shard0/shard1/shard2), never on primary. `id` is a
 * client-generated UUID (see AuthService.register), assigned in application
 * code *before* the shard is resolved, since the shard is a function of
 * this id.
 *
 * <p>passwordHash is annotated @JsonIgnore so it can never leak through any
 * endpoint that returns a User directly (e.g. GET /users/me) - the original
 * achieves the same end by manually destructuring it out of the response.
 *
 * <p>Implements {@link Persistable}: since the id is assigned in
 * application code (not DB/Hibernate generated), Spring Data JPA's default
 * "is the id null?" new-vs-existing check would otherwise see a non-null id
 * on first save and call {@code entityManager.merge()} (an unnecessary
 * SELECT-then-INSERT) instead of {@code persist()}. The isNew flag defaults
 * to true for a freshly-constructed instance and flips to false once
 * Hibernate has loaded (@PostLoad) or persisted (@PostPersist) it, so
 * UsersService.updateProfile's load-then-save correctly performs an UPDATE.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User implements Persistable<UUID> {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Transient
  private boolean isNew = true;

  @Column(nullable = false, unique = true)
  private String email;

  @JsonIgnore
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "full_name", nullable = false)
  private String fullName;

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
