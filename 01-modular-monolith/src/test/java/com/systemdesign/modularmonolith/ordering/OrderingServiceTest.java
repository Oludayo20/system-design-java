package com.systemdesign.modularmonolith.ordering;

import com.systemdesign.modularmonolith.basket.BasketService;
import com.systemdesign.modularmonolith.basket.dto.BasketLine;
import com.systemdesign.modularmonolith.basket.dto.BasketView;
import com.systemdesign.modularmonolith.infrastructure.rabbitmq.EventBus;
import com.systemdesign.modularmonolith.infrastructure.rabbitmq.RabbitMqConstants;
import com.systemdesign.modularmonolith.ordering.dto.OrderCreatedEvent;
import com.systemdesign.modularmonolith.ordering.dto.PlaceOrderResult;
import com.systemdesign.modularmonolith.ordering.entity.Order;
import com.systemdesign.modularmonolith.ordering.entity.OrderItem;
import com.systemdesign.modularmonolith.ordering.entity.OrderStatus;
import com.systemdesign.modularmonolith.ordering.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ports {@code src/modules/ordering/ordering.service.spec.ts}. The NestJS original mocks
 * TypeORM's {@code DataSource#transaction} and asserts against it directly; here the transaction
 * boundary is just {@code @Transactional} on {@link OrderingService#placeOrder}, so there is
 * nothing to mock for it -- the two behaviors actually under test (basket is asserted non-empty
 * and cleared, and {@code order.created} is published with a snapshot of the saved order) map
 * directly onto {@link OrderRepository#save} and {@link EventBus#publish}.
 */
@ExtendWith(MockitoExtension.class)
class OrderingServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_1 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PRODUCT_2 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock
    private OrderRepository orders;

    @Mock
    private BasketService basketService;

    @Mock
    private EventBus eventBus;

    private OrderingService orderingService;

    @BeforeEach
    void setUp() {
        orderingService = new OrderingService(orders, basketService, eventBus);
    }

    private static BasketView twoItemBasket() {
        BasketLine mouse = new BasketLine(PRODUCT_1, "Wireless Mouse", new BigDecimal("39.99"), 1, new BigDecimal("39.99"));
        BasketLine keyboard = new BasketLine(PRODUCT_2, "Mechanical Keyboard", new BigDecimal("49.99"), 1, new BigDecimal("49.99"));
        return new BasketView(USER_ID, List.of(mouse, keyboard), new BigDecimal("89.98"));
    }

    private static Order savedOrderWithId(UUID orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PLACED);
        order.setTotal(new BigDecimal("89.98"));

        OrderItem mouseItem = new OrderItem();
        mouseItem.setProductId(PRODUCT_1);
        mouseItem.setProductName("Wireless Mouse");
        mouseItem.setUnitPrice(new BigDecimal("39.99"));
        mouseItem.setQuantity(1);
        mouseItem.setLineTotal(new BigDecimal("39.99"));
        order.addItem(mouseItem);

        OrderItem keyboardItem = new OrderItem();
        keyboardItem.setProductId(PRODUCT_2);
        keyboardItem.setProductName("Mechanical Keyboard");
        keyboardItem.setUnitPrice(new BigDecimal("49.99"));
        keyboardItem.setQuantity(1);
        keyboardItem.setLineTotal(new BigDecimal("49.99"));
        order.addItem(keyboardItem);

        return order;
    }

    @Test
    void persistsTheOrderAndClearsTheBasket() {
        UUID orderId = UUID.randomUUID();
        when(basketService.assertNotEmpty(USER_ID)).thenReturn(twoItemBasket());
        when(orders.save(any(Order.class))).thenReturn(savedOrderWithId(orderId));
        when(eventBus.publish(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        PlaceOrderResult result = orderingService.placeOrder(USER_ID);

        assertThat(result).isEqualTo(new PlaceOrderResult(true, orderId));
        verify(basketService).assertNotEmpty(USER_ID);
        verify(basketService).clear(USER_ID);
    }

    @Test
    void publishesOrderCreatedWithASnapshotOfTheOrderAfterItIsSaved() {
        UUID orderId = UUID.randomUUID();
        when(basketService.assertNotEmpty(USER_ID)).thenReturn(twoItemBasket());
        when(orders.save(any(Order.class))).thenReturn(savedOrderWithId(orderId));
        when(eventBus.publish(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        orderingService.placeOrder(USER_ID);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventBus).publish(eq(RabbitMqConstants.ORDER_CREATED), eventCaptor.capture());

        OrderCreatedEvent published = eventCaptor.getValue();
        assertThat(published.orderId()).isEqualTo(orderId);
        assertThat(published.userId()).isEqualTo(USER_ID);
        assertThat(published.total()).isEqualByComparingTo("89.98");
        assertThat(published.items()).containsExactly(
                new OrderCreatedEvent.Item(PRODUCT_1, "Wireless Mouse", 1, new BigDecimal("39.99")),
                new OrderCreatedEvent.Item(PRODUCT_2, "Mechanical Keyboard", 1, new BigDecimal("49.99")));
    }
}
