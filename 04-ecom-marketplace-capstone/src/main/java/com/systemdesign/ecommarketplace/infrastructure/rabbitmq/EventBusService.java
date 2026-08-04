package com.systemdesign.ecommarketplace.infrastructure.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Mirrors src/infrastructure/rabbitmq/event-bus.service.ts. The only thing
 * OrdersService (and any future publisher) depends on. Kept as a narrow
 * class wrapping RabbitTemplate rather than injecting RabbitTemplate
 * everywhere, so unit tests can mock {@link #publish} without dragging in a
 * real AMQP connection - this is what makes the "persist-then-publish,
 * without awaiting downstream workers" unit test possible without Docker.
 */
@Service
public class EventBusService {

  private static final Logger log = LoggerFactory.getLogger(EventBusService.class);

  private final RabbitTemplate rabbitTemplate;

  public EventBusService(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publish(String routingKey, Object payload) {
    log.info("Publishing {} -> {}", routingKey, RabbitMQConstants.DOMAIN_EVENTS_EXCHANGE);
    rabbitTemplate.convertAndSend(RabbitMQConstants.DOMAIN_EVENTS_EXCHANGE, routingKey, payload);
  }
}
