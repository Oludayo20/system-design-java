package com.systemdesign.ecommarketplace.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.systemdesign.ecommarketplace.common.exceptions.BadRequestException;
import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.EventBusService;
import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants;
import com.systemdesign.ecommarketplace.marketplace.MarketplaceService;
import com.systemdesign.ecommarketplace.marketplace.dto.ProductForOrder;
import com.systemdesign.ecommarketplace.orders.dto.CreateOrderItemRequest;
import com.systemdesign.ecommarketplace.orders.dto.CreateOrderRequest;
import com.systemdesign.ecommarketplace.orders.dto.CreateOrderResult;
import com.systemdesign.ecommarketplace.orders.entity.Order;
import com.systemdesign.ecommarketplace.orders.entity.OrderStatus;
import com.systemdesign.ecommarketplace.orders.repository.OrderRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ported from src/modules/orders/orders.service.spec.ts. Proves the doc.md
 * order flow without touching Postgres/RabbitMQ: persist the order, THEN
 * publish order.created, THEN return - with nothing in between awaiting a
 * worker (there's nothing to await; the four workers are separate queue
 * consumers entirely outside this call stack).
 *
 * <p>Deviation from the original: the TS spec uses the bare string
 * "user-123" as a userId fixture, which is valid in TypeORM's loosely typed
 * world. OrdersService.createOrder here calls UUID.fromString(userId), so
 * this port uses a syntactically valid UUID fixture instead - the scenarios
 * and assertions are otherwise identical.
 */
class OrdersServiceTest {

  private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
  private static final UUID PRODUCT_ID = UUID.fromString("a1b2c3d4-e5f6-4789-a012-3456789abcde");

  private TransactionTemplate transactionTemplate;
  private OrderRepository orderRepository;
  private MarketplaceService marketplaceService;
  private EventBusService eventBus;
  private OrdersService service;
  private Order savedOrder;

  @BeforeEach
  void setUp() {
    savedOrder = new Order();
    savedOrder.setId(UUID.fromString("99999999-9999-9999-9999-999999999999"));
    savedOrder.setUserId(UUID.fromString(USER_ID));
    savedOrder.setStatus(OrderStatus.PENDING);
    savedOrder.setTotalCents(2000);
    savedOrder.setCreatedAt(OffsetDateTime.parse("2024-01-01T00:00:00Z"));

    transactionTemplate = mock(TransactionTemplate.class);
    orderRepository = mock(OrderRepository.class);
    marketplaceService = mock(MarketplaceService.class);
    eventBus = mock(EventBusService.class);

    // Mirrors the original's `dataSource.transaction = jest.fn(cb => cb(manager))`:
    // immediately invoke the callback and return its result, so the
    // service's persist step actually runs against the mocked repository.
    when(transactionTemplate.execute(any())).thenAnswer(this::runCallback);

    when(marketplaceService.getProductForOrder(PRODUCT_ID))
        .thenReturn(new ProductForOrder(PRODUCT_ID, "Wireless Earbuds", 1000, 10));
    when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

    service = new OrdersService(transactionTemplate, orderRepository, marketplaceService, eventBus);
  }

  @SuppressWarnings("unchecked")
  private Object runCallback(org.mockito.invocation.InvocationOnMock invocation) {
    TransactionCallback<Object> callback = invocation.getArgument(0);
    return callback.doInTransaction(mock(TransactionStatus.class));
  }

  @Test
  @DisplayName("persists the order before publishing order.created, and returns without waiting on workers")
  void persistsBeforePublishing() {
    CreateOrderRequest dto = new CreateOrderRequest(List.of(new CreateOrderItemRequest(PRODUCT_ID, 2)));

    CreateOrderResult result = service.createOrder(USER_ID, dto);

    assertThat(result).isEqualTo(new CreateOrderResult(true, savedOrder.getId().toString()));
    verify(transactionTemplate).execute(any());
    verify(eventBus)
        .publish(
            eq(RabbitMQConstants.ROUTING_KEY_ORDER_CREATED),
            any());

    // persist-then-publish, not the other way around
    InOrder order = inOrder(orderRepository, eventBus);
    order.verify(orderRepository).save(any(Order.class));
    order.verify(eventBus).publish(eq(RabbitMQConstants.ROUTING_KEY_ORDER_CREATED), any());
  }

  @Test
  @DisplayName("validates against Marketplace via the narrow getProductForOrder method, never touching Product rows directly")
  void validatesViaNarrowMethod() {
    CreateOrderRequest dto = new CreateOrderRequest(List.of(new CreateOrderItemRequest(PRODUCT_ID, 3)));

    service.createOrder(USER_ID, dto);

    verify(marketplaceService).getProductForOrder(PRODUCT_ID);
    // OrdersService must never call Marketplace's stock-mutating method -
    // that belongs solely to the Inventory worker, reacting to the event.
    verify(marketplaceService, never()).decrementStock(any(), anyInt());
  }

  @Test
  @DisplayName("rejects the order before any persistence or publish when stock is insufficient")
  void rejectsInsufficientStock() {
    when(marketplaceService.getProductForOrder(PRODUCT_ID))
        .thenReturn(new ProductForOrder(PRODUCT_ID, "Wireless Earbuds", 1000, 1));
    CreateOrderRequest dto = new CreateOrderRequest(List.of(new CreateOrderItemRequest(PRODUCT_ID, 5)));

    assertThatThrownBy(() -> service.createOrder(USER_ID, dto)).isInstanceOf(BadRequestException.class);

    verify(transactionTemplate, never()).execute(any());
    verify(eventBus, never()).publish(any(), any());
  }
}
