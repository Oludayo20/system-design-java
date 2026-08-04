package com.systemdesign.asyncqueue.rabbitmq;

import java.net.URI;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the RabbitMQ {@link ConnectionFactory} from a single {@code RABBITMQ_URL}
 * (amqp://user:pass@host:port), same as the original's {@code amqp.connect([url])}. Parsed by
 * hand rather than relying on Spring Boot's {@code spring.rabbitmq.addresses} property so the
 * `.env` shape stays identical to the NestJS project (one URL, not four separate host/port/user/
 * password properties).
 *
 * <p>Declaring this bean here means Spring Boot's {@code RabbitAutoConfiguration} backs off its
 * own {@code ConnectionFactory} (it is {@code @ConditionalOnMissingBean}) but still supplies the
 * {@code RabbitTemplate} and {@code RabbitAdmin} beans wired to this one — those beans in turn
 * auto-declare every {@link org.springframework.amqp.core.Declarable} bean in
 * {@link RabbitTopologyConfig}.
 */
@Configuration
public class RabbitConnectionConfig {

    @Bean
    public ConnectionFactory connectionFactory(@Value("${rabbitmq.url}") String rabbitmqUrl) {
        URI uri = URI.create(rabbitmqUrl);

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(uri.getHost());
        connectionFactory.setPort(uri.getPort() == -1 ? 5672 : uri.getPort());

        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            String[] parts = userInfo.split(":", 2);
            connectionFactory.setUsername(parts[0]);
            if (parts.length > 1) {
                connectionFactory.setPassword(parts[1]);
            }
        }

        String vhost = uri.getPath();
        if (vhost != null && vhost.length() > 1) {
            connectionFactory.setVirtualHost(vhost.substring(1));
        }

        return connectionFactory;
    }
}
