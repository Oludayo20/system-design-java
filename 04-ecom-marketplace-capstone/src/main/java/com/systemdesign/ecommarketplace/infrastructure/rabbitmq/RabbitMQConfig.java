package com.systemdesign.ecommarketplace.infrastructure.rabbitmq;

import static com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants.DOMAIN_EVENTS_DLX;
import static com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants.DOMAIN_EVENTS_EXCHANGE;
import static com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants.QUEUE_ANALYTICS_ON_ORDER_CREATED;
import static com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants.QUEUE_EMAIL_ON_ORDER_CREATED;
import static com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants.QUEUE_INVENTORY_ON_ORDER_CREATED;
import static com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants.QUEUE_WALLET_SETTLEMENT_ON_ORDER_CREATED;
import static com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants.ROUTING_KEY_ORDER_CREATED;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mirrors src/infrastructure/rabbitmq/rabbitmq.module.ts: a durable topic
 * exchange `domain_events`, a dead-letter exchange `domain_events.dlx`, and
 * one durable queue per worker, each bound to `order.created` and
 * configured with `x-dead-letter-exchange` so a nacked-without-requeue
 * message lands on the DLX instead of vanishing - same retry/DLQ shape as
 * project 03 (async queue processing), kept minimal for this demo.
 *
 * <p>ConnectionFactory is built by hand from a single AMQP URI
 * (RABBITMQ_URL) rather than Spring Boot's spring.rabbitmq.host/port/
 * username/password properties, to match the original's single-URL
 * configuration style (.env.example's RABBITMQ_URL=amqp://user:pass@host:port).
 */
@Configuration
public class RabbitMQConfig {

  @Bean
  public ConnectionFactory connectionFactory(@Value("${app.rabbitmq.url}") String rabbitmqUrl) throws Exception {
    com.rabbitmq.client.ConnectionFactory rabbitClientFactory = new com.rabbitmq.client.ConnectionFactory();
    rabbitClientFactory.setUri(rabbitmqUrl);
    return new CachingConnectionFactory(rabbitClientFactory);
  }

  @Bean
  public MessageConverter messageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter);
    return template;
  }

  /**
   * Explicit bean named "rabbitListenerContainerFactory" (Spring Boot's
   * default @RabbitListener container factory name) so every
   * @RabbitListener in the app (Email/Inventory/Analytics/Wallet workers)
   * deserializes OrderCreatedEvent via the same Jackson converter used to
   * publish it, without each listener needing its own containerFactory
   * attribute.
   */
  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory, MessageConverter messageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(messageConverter);
    return factory;
  }

  @Bean
  public TopicExchange domainEventsExchange() {
    return org.springframework.amqp.core.ExchangeBuilder.topicExchange(DOMAIN_EVENTS_EXCHANGE).durable(true).build();
  }

  @Bean
  public TopicExchange domainEventsDlx() {
    return org.springframework.amqp.core.ExchangeBuilder.topicExchange(DOMAIN_EVENTS_DLX).durable(true).build();
  }

  @Bean
  public Queue emailOrderCreatedQueue() {
    return durableQueueWithDlx(QUEUE_EMAIL_ON_ORDER_CREATED);
  }

  @Bean
  public Queue inventoryOrderCreatedQueue() {
    return durableQueueWithDlx(QUEUE_INVENTORY_ON_ORDER_CREATED);
  }

  @Bean
  public Queue analyticsOrderCreatedQueue() {
    return durableQueueWithDlx(QUEUE_ANALYTICS_ON_ORDER_CREATED);
  }

  @Bean
  public Queue walletSettlementOrderCreatedQueue() {
    return durableQueueWithDlx(QUEUE_WALLET_SETTLEMENT_ON_ORDER_CREATED);
  }

  @Bean
  public Binding emailOrderCreatedBinding(Queue emailOrderCreatedQueue, TopicExchange domainEventsExchange) {
    return BindingBuilder.bind(emailOrderCreatedQueue).to(domainEventsExchange).with(ROUTING_KEY_ORDER_CREATED);
  }

  @Bean
  public Binding inventoryOrderCreatedBinding(
      Queue inventoryOrderCreatedQueue, TopicExchange domainEventsExchange) {
    return BindingBuilder.bind(inventoryOrderCreatedQueue)
        .to(domainEventsExchange)
        .with(ROUTING_KEY_ORDER_CREATED);
  }

  @Bean
  public Binding analyticsOrderCreatedBinding(
      Queue analyticsOrderCreatedQueue, TopicExchange domainEventsExchange) {
    return BindingBuilder.bind(analyticsOrderCreatedQueue)
        .to(domainEventsExchange)
        .with(ROUTING_KEY_ORDER_CREATED);
  }

  @Bean
  public Binding walletSettlementOrderCreatedBinding(
      Queue walletSettlementOrderCreatedQueue, TopicExchange domainEventsExchange) {
    return BindingBuilder.bind(walletSettlementOrderCreatedQueue)
        .to(domainEventsExchange)
        .with(ROUTING_KEY_ORDER_CREATED);
  }

  private Queue durableQueueWithDlx(String name) {
    return QueueBuilder.durable(name).withArgument("x-dead-letter-exchange", DOMAIN_EVENTS_DLX).build();
  }
}
