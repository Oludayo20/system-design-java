package com.systemdesign.ecommarketplace.orders;

import com.systemdesign.ecommarketplace.common.exceptions.BadRequestException;
import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.EventBusService;
import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.OrderCreatedEvent;
import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants;
import com.systemdesign.ecommarketplace.marketplace.MarketplaceService;
import com.systemdesign.ecommarketplace.marketplace.dto.ProductForOrder;
import com.systemdesign.ecommarketplace.orders.dto.CreateOrderRequest;
import com.systemdesign.ecommarketplace.orders.dto.CreateOrderResult;
import com.systemdesign.ecommarketplace.orders.entity.Order;
import com.systemdesign.ecommarketplace.orders.entity.OrderItem;
import com.systemdesign.ecommarketplace.orders.entity.OrderStatus;
import com.systemdesign.ecommarketplace.orders.repository.OrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Mirrors src/modules/orders/orders.service.ts. */
@Service
public class OrdersService {

  private static final Logger log = LoggerFactory.getLogger(OrdersService.class);

  private final TransactionTemplate primaryTransactionTemplate;
  private final OrderRepository orderRepository;
  private final MarketplaceService marketplaceService;
  private final EventBusService eventBus;

  public OrdersService(
      @Qualifier("primaryTransactionTemplate") TransactionTemplate primaryTransactionTemplate,
      OrderRepository orderRepository,
      MarketplaceService marketplaceService,
      EventBusService eventBus) {
    this.primaryTransactionTemplate = primaryTransactionTemplate;
    this.orderRepository = orderRepository;
    this.marketplaceService = marketplaceService;
    this.eventBus = eventBus;
  }

  /**
   * The doc.md flow, made real: validate -> persist in a transaction ->
   * publish order.created -> return immediately. Nothing here awaits the
   * Email/Inventory/Analytics/Wallet workers - they are separate consumers
   * on separate queues, running in parallel, entirely decoupled from this
   * request/response cycle. The only module boundary crossed synchronously
   * is the read-only MarketplaceService.getProductForOrder call, needed to
   * validate the order before money/stock are ever touched.
   */
  public CreateOrderResult createOrder(String userId, CreateOrderRequest dto) {
    List<ValidatedItem> validatedItems = new ArrayList<>();

    for (var item : dto.items()) {
      ProductForOrder product = marketplaceService.getProductForOrder(item.productId());
      if (product.stock() < item.quantity()) {
        throw new BadRequestException(
            "Insufficient stock for \""
                + product.name()
                + "\": requested "
                + item.quantity()
                + ", available "
                + product.stock());
      }
      validatedItems.add(
          new ValidatedItem(product.id(), product.name(), item.quantity(), product.priceCents()));
    }

    int totalCents = validatedItems.stream().mapToInt(i -> i.unitPriceCents() * i.quantity()).sum();

    Order savedOrder =
        primaryTransactionTemplate.execute(
            status -> {
              Order order = new Order();
              order.setId(UUID.randomUUID());
              order.setUserId(UUID.fromString(userId));
              order.setStatus(OrderStatus.PENDING);
              order.setTotalCents(totalCents);

              List<OrderItem> items = new ArrayList<>();
              for (ValidatedItem vi : validatedItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setId(UUID.randomUUID());
                orderItem.setProductId(vi.productId());
                orderItem.setProductName(vi.productName());
                orderItem.setQuantity(vi.quantity());
                orderItem.setUnitPriceCents(vi.unitPriceCents());
                orderItem.setOrder(order);
                items.add(orderItem);
              }
              order.setItems(items);

              return orderRepository.save(order);
            });

    log.info("Order {} persisted for user {} - publishing order.created", savedOrder.getId(), userId);

    List<OrderCreatedEvent.Item> eventItems =
        validatedItems.stream()
            .map(vi -> new OrderCreatedEvent.Item(vi.productId().toString(), vi.quantity(), vi.unitPriceCents()))
            .toList();
    OrderCreatedEvent event =
        new OrderCreatedEvent(
            savedOrder.getId().toString(),
            userId,
            savedOrder.getTotalCents(),
            eventItems,
            savedOrder.getCreatedAt().toString());
    eventBus.publish(RabbitMQConstants.ROUTING_KEY_ORDER_CREATED, event);

    return new CreateOrderResult(true, savedOrder.getId().toString());
  }

  private record ValidatedItem(UUID productId, String productName, int quantity, int unitPriceCents) {}
}
