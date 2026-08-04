package com.systemdesign.modularmonolith.ordering.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mirrors {@code src/modules/ordering/entities/order-item.entity.ts} ({@code ordering.order_items}).
 *
 * <p>{@code order} (the relation) is the writable/owning side of the {@code order_id} foreign
 * key -- {@code OrderingService} sets it via {@code Order#addItem} and relies on JPA cascading the
 * save from the parent {@code Order}, the same way the source relies on TypeORM's
 * {@code cascade: true} on {@code Order#items}. The plain {@code orderId} column is therefore
 * read-only in Java ({@code insertable=false, updatable=false}) -- it's populated on read, not
 * used for writes. No FK/relation into {@code catalog.products}: this is a point-in-time
 * snapshot, not a live reference.</p>
 */
@Entity
@Table(name = "order_items", schema = "ordering")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @Column(name = "order_id", insertable = false, updatable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;
}
