package com.systemdesign.freshcart.orderapi.orders;

import com.systemdesign.freshcart.orderapi.orders.dto.CreateOrderDto;
import com.systemdesign.freshcart.orderapi.orders.dto.OrderItemDto;
import com.systemdesign.freshcart.orderapi.orders.dto.OrderResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.systemdesign.freshcart.orderapi.rabbitmq.RabbitMqConstants.GROCERY_EVENTS_EXCHANGE;
import static com.systemdesign.freshcart.orderapi.rabbitmq.RabbitMqConstants.ORDER_PLACED_ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves publish-after-commit by construction, mirroring the TypeScript sibling's
 * {@code orders.service.spec.ts}: the RabbitMQ publish call only happens after the (mocked)
 * transactional save has returned — i.e. after the transaction the mock stands in for has
 * committed.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID ORDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AmqpTemplate rabbitTemplate;

    private TransactionTemplate transactionTemplate;
    private OrderService orderService;
    private CreateOrderDto dto;

    @BeforeEach
    void setUp() {
        // A TransactionTemplate stand-in that just invokes the callback synchronously — good
        // enough to prove ordering (save must finish before the callback returns) without a real
        // PlatformTransactionManager/DataSource. A real subclass override (not a Mockito mock of
        // this concrete class) so the test doesn't depend on Mockito's inline mock maker being
        // able to instrument java.lang/Spring framework classes on whatever JDK runs the build.
        transactionTemplate = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(null);
            }
        };

        orderService = new OrderService(orderRepository, transactionTemplate, rabbitTemplate);

        dto = new CreateOrderDto("customer-42", List.of(
                new OrderItemDto("milk-1l", "Whole Milk 1L", 2, new BigDecimal("1.50")),
                new OrderItemDto("bread-1", "Sliced White Bread", 1, new BigDecimal("2.00"))));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            order.setCreatedAt(CREATED_AT);
            return order;
        });
    }

    @Test
    void persistsTheOrderAndReturnsItImmediately() {
        OrderResponseDto result = orderService.placeOrder(dto);

        verify(orderRepository, times(1)).save(any(Order.class));
        assertThat(result.id()).isEqualTo(ORDER_ID);
        assertThat(result.totalAmount()).isEqualByComparingTo("5.00");
        assertThat(result.status()).isEqualTo("placed");
    }

    @Test
    void publishesOrderPlacedToTheGroceryEventsExchangeWithTheOrderPayload() {
        orderService.placeOrder(dto);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(GROCERY_EVENTS_EXCHANGE), eq(ORDER_PLACED_ROUTING_KEY), payloadCaptor.capture(),
                any(MessagePostProcessor.class));

        OrderPlacedEvent event = (OrderPlacedEvent) payloadCaptor.getValue();
        assertThat(event.eventType()).isEqualTo("order.placed");
        assertThat(event.eventId()).isNotNull();
        assertThat(event.payload().orderId()).isEqualTo(ORDER_ID);
        assertThat(event.payload().customerId()).isEqualTo("customer-42");
        assertThat(event.payload().totalAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    void savesInsideTheTransactionBeforePublishingAfterItCommits() {
        orderService.placeOrder(dto);

        InOrder order = inOrder(orderRepository, rabbitTemplate);
        order.verify(orderRepository).save(any(Order.class));
        order.verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(), any(MessagePostProcessor.class));
    }
}
