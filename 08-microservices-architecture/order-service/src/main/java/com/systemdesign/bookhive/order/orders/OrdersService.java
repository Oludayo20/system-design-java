package com.systemdesign.bookhive.order.orders;

import com.systemdesign.bookhive.order.catalog.CatalogClient;
import com.systemdesign.bookhive.order.catalog.ReservationResult;
import com.systemdesign.bookhive.order.notification.NotificationClient;
import com.systemdesign.bookhive.order.orders.dto.CreateOrderRequest;
import com.systemdesign.bookhive.order.orders.entity.Order;
import com.systemdesign.bookhive.order.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrdersService {

    private static final Logger log = LoggerFactory.getLogger(OrdersService.class);

    private final OrderRepository orders;
    private final CatalogClient catalogClient;
    private final NotificationClient notificationClient;

    public OrdersService(OrderRepository orders, CatalogClient catalogClient, NotificationClient notificationClient) {
        this.orders = orders;
        this.catalogClient = catalogClient;
        this.notificationClient = notificationClient;
    }

    /**
     * The whole point of this service, laid out step by step:
     * <ol>
     *   <li>Ask catalog-service (HTTP) for price + stock, and have it atomically decrement
     *       stock. This can fail the whole request (404 unknown book, 409 out of stock, 502
     *       catalog unreachable) - that's correct, an order without a real reservation isn't
     *       valid.</li>
     *   <li>Persist the order in order-db - the ONLY database this service writes to.</li>
     *   <li>Fire-and-forget notify notification-service. This step can fail silently; see
     *       {@link NotificationClient} for why.</li>
     * </ol>
     */
    public Order placeOrder(UUID userId, CreateOrderRequest dto, String authorizationHeader) {
        ReservationResult reservation = catalogClient.reserveStock(dto.bookId(), dto.quantity(), authorizationHeader);

        Order order = new Order();
        order.setUserId(userId);
        order.setBookId(dto.bookId());
        order.setQuantity(dto.quantity());
        order.setUnitPriceCents(reservation.unitPriceCents());
        order.setTotalCents(reservation.totalCents());
        order.setStatus("confirmed");
        order = orders.save(order);

        log.info("Order {} confirmed for user {} (book {})", order.getId(), userId, dto.bookId());

        // Not in the response's success path - placeOrder's own success does not depend on it
        // (see NotificationClient: every failure mode is swallowed). Called synchronously here
        // only so the outcome gets logged before the response is written.
        notificationClient.notifyOrderCreated(order.getId(), userId, dto.bookId(), dto.quantity());

        return order;
    }

    public List<Order> findAllForUser(UUID userId) {
        return orders.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order findOneForUser(UUID id, UUID userId) {
        return orders.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
    }
}
