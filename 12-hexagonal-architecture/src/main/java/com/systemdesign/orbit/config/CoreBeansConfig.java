package com.systemdesign.orbit.config;

import com.systemdesign.orbit.adapters.out.notification.ConsoleNotifierAdapter;
import com.systemdesign.orbit.adapters.out.payment.FlutterwaveMockAdapter;
import com.systemdesign.orbit.adapters.out.payment.StripeMockAdapter;
import com.systemdesign.orbit.adapters.out.persistence.InMemorySubscriptionRepository;
import com.systemdesign.orbit.adapters.out.persistence.PostgresSubscriptionRepository;
import com.systemdesign.orbit.adapters.out.persistence.SubscriptionJpaRepository;
import com.systemdesign.orbit.core.application.CancelUseCase;
import com.systemdesign.orbit.core.application.ChangePlanUseCase;
import com.systemdesign.orbit.core.application.GetSubscriptionUseCase;
import com.systemdesign.orbit.core.application.SubscribeUseCase;
import com.systemdesign.orbit.core.ports.out.NotifierPort;
import com.systemdesign.orbit.core.ports.out.PaymentGatewayPort;
import com.systemdesign.orbit.core.ports.out.SubscriptionRepositoryPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is where the dependency-inversion wiring the whole project demonstrates happens: {@code
 * app.repository} and {@code app.payment-provider} (read from {@code APP_REPOSITORY} /
 * {@code APP_PAYMENT_PROVIDER} env vars, see application.yml) decide which concrete adapter class
 * gets bound to each output port interface the core defined for itself. Swap either property and
 * restart — the number of files inside {@code core/} that change is zero.
 *
 * <p>The use-case classes (core/application) are plain Java with no Spring annotations, so they
 * can't be discovered by component scanning — each is registered here with an explicit
 * {@code @Bean} method that injects the port interfaces above. Both {@code
 * SubscriptionController} (HTTP) and {@code OrbitCliRunner} (CLI) then @Autowire these same
 * singleton use-case beans, which is what proves both inbound adapters drive identical core
 * logic.
 */
@Configuration
public class CoreBeansConfig {

    @Bean
    public SubscriptionRepositoryPort subscriptionRepositoryPort(
            @Value("${app.repository:memory}") String repositoryKind,
            ObjectProvider<SubscriptionJpaRepository> jpaRepositoryProvider) {
        if ("postgres".equalsIgnoreCase(repositoryKind)) {
            SubscriptionJpaRepository jpaRepository = jpaRepositoryProvider.getIfAvailable();
            if (jpaRepository == null) {
                throw new IllegalStateException(
                        "app.repository=postgres but no SubscriptionJpaRepository bean is available. "
                                + "Set APP_REPOSITORY=postgres so OrbitApplication activates the 'postgres' "
                                + "Spring profile (see config.PostgresPersistenceConfig).");
            }
            return new PostgresSubscriptionRepository(jpaRepository);
        }
        return new InMemorySubscriptionRepository();
    }

    @Bean
    public PaymentGatewayPort paymentGatewayPort(@Value("${app.payment-provider:stripe}") String provider) {
        return "flutterwave".equalsIgnoreCase(provider) ? new FlutterwaveMockAdapter() : new StripeMockAdapter();
    }

    @Bean
    public NotifierPort notifierPort() {
        return new ConsoleNotifierAdapter();
    }

    @Bean
    public SubscribeUseCase subscribeUseCase(
            SubscriptionRepositoryPort repository, PaymentGatewayPort paymentGateway, NotifierPort notifier) {
        return new SubscribeUseCase(repository, paymentGateway, notifier);
    }

    @Bean
    public ChangePlanUseCase changePlanUseCase(
            SubscriptionRepositoryPort repository, PaymentGatewayPort paymentGateway, NotifierPort notifier) {
        return new ChangePlanUseCase(repository, paymentGateway, notifier);
    }

    @Bean
    public CancelUseCase cancelUseCase(SubscriptionRepositoryPort repository, NotifierPort notifier) {
        return new CancelUseCase(repository, notifier);
    }

    @Bean
    public GetSubscriptionUseCase getSubscriptionUseCase(SubscriptionRepositoryPort repository) {
        return new GetSubscriptionUseCase(repository);
    }
}
