package com.systemdesign.ecommarketplace.orders.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * Mirrors src/modules/orders/entities/order.entity.ts. Lives on the PRIMARY
 * database.
 *
 * <p>No FK/relation to User: the owning User lives on a shard database, a
 * physically different Postgres instance from this (primary) database.
 * Cross-database foreign keys aren't possible, so referential integrity for
 * userId is enforced in application code (JWT-authenticated at write time)
 * rather than by the database - same as the original.
 *
 * <p>Implements {@link Persistable} because the id is assigned by
 * application code (OrdersService calls {@code UUID.randomUUID()} before
 * save - see OrdersService's class comment on why every entity's id is
 * assigned this way rather than left to a DB default). Without this,
 * Spring Data JPA's default new-vs-existing check ("is the id null?") would
 * see a non-null id and call {@code entityManager.merge()} instead of
 * {@code persist()} on first save - which would silently skip the
 * {@code cascade = PERSIST} on {@code items} below (merge only cascades
 * cascade=MERGE/ALL), so OrderItems would never be written. The isNew flag
 * defaults to true for a freshly-constructed instance and flips to false
 * once Hibernate has loaded (@PostLoad) or persisted (@PostPersist) it, so
 * a subsequent save() of the same loaded instance correctly merges/updates
 * instead of re-inserting.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order implements Persistable<UUID> {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Transient
  private boolean isNew = true;

  @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "order_status_enum")
  private OrderStatus status = OrderStatus.PENDING;

  @Column(name = "total_cents", nullable = false)
  private int totalCents;

  // Never serialized: no endpoint returns an Order with its items inline
  // (OrdersController only ever returns { success, orderId }); @JsonIgnore
  // is defensive.
  @JsonIgnore
  @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
  private List<OrderItem> items = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PostLoad
  @PostPersist
  void markNotNew() {
    this.isNew = false;
  }
}
