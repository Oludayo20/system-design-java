package com.systemdesign.orbit.adapters.in.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemdesign.orbit.adapters.in.http.dto.ChangePlanResponse;
import com.systemdesign.orbit.adapters.in.http.dto.SubscriptionResponse;
import com.systemdesign.orbit.core.application.CancelUseCase;
import com.systemdesign.orbit.core.application.ChangePlanUseCase;
import com.systemdesign.orbit.core.application.GetSubscriptionUseCase;
import com.systemdesign.orbit.core.application.SubscribeUseCase;
import com.systemdesign.orbit.core.domain.DomainError;
import com.systemdesign.orbit.core.domain.Subscription;
import com.systemdesign.orbit.core.ports.in.CancelPort.CancelCommand;
import com.systemdesign.orbit.core.ports.in.ChangePlanPort.ChangePlanCommand;
import com.systemdesign.orbit.core.ports.in.ChangePlanPort.ChangePlanResult;
import com.systemdesign.orbit.core.ports.in.GetSubscriptionPort.GetSubscriptionQuery;
import com.systemdesign.orbit.core.ports.in.SubscribePort.SubscribeCommand;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Inbound/driving adapter #2. Same core, same four use-case beans as the REST controller
 * (adapters/in/http/SubscriptionController.java) — only the "who's calling" changes. Gated
 * behind the "cli" Spring profile so it never runs during a normal {@code docker compose up}.
 *
 * <p>Run it with:
 *
 * <pre>
 *   mvn spring-boot:run -Dspring-boot.run.profiles=cli \
 *       -Dspring-boot.run.arguments="subscribe --customer=c1 --plan=pro"
 *   mvn spring-boot:run -Dspring-boot.run.profiles=cli \
 *       -Dspring-boot.run.arguments="change-plan --id=&lt;id&gt; --plan=enterprise"
 *   mvn spring-boot:run -Dspring-boot.run.profiles=cli \
 *       -Dspring-boot.run.arguments="cancel --id=&lt;id&gt;"
 *   mvn spring-boot:run -Dspring-boot.run.profiles=cli \
 *       -Dspring-boot.run.arguments="get --id=&lt;id&gt;"
 * </pre>
 *
 * <p>{@code spring-boot.run.arguments} is SPACE-delimited (per the Spring Boot Maven Plugin's own
 * docs), not comma-delimited — wrap the whole value in quotes so the shell treats it as one
 * property.
 *
 * <p>Implements {@link ApplicationRunner} (not the simpler {@code CommandLineRunner}) so
 * arguments are read via Spring's own {@link ApplicationArguments}, which separates "option
 * args" (anything shaped {@code --key=value}, including Spring's own {@code
 * --spring.profiles.active=cli} that {@code -Dspring-boot.run.profiles} injects as a plain
 * program argument) from "non-option args" (the bare command word). Parsing raw {@code
 * String[] args} by position would mistake {@code --spring.profiles.active=cli} for the command.
 *
 * <p>{@code APP_REPOSITORY=memory} (default) gives the CLI its own private, in-process store —
 * good for a quick standalone check, but state doesn't survive past this one process. Add the
 * "postgres" profile too (e.g. {@code -Dspring-boot.run.profiles=cli,postgres} with
 * {@code POSTGRES_HOST} etc. pointing at the same database the HTTP API uses) to have the CLI
 * read/write the exact same rows the HTTP API does — the strongest proof that both adapters
 * drive identical core logic.
 */
@Component
@Profile("cli")
public class OrbitCliRunner implements ApplicationRunner {

    private final SubscribeUseCase subscribeUseCase;
    private final ChangePlanUseCase changePlanUseCase;
    private final CancelUseCase cancelUseCase;
    private final GetSubscriptionUseCase getSubscriptionUseCase;
    private final ObjectMapper objectMapper;

    public OrbitCliRunner(
            SubscribeUseCase subscribeUseCase,
            ChangePlanUseCase changePlanUseCase,
            CancelUseCase cancelUseCase,
            GetSubscriptionUseCase getSubscriptionUseCase,
            ObjectMapper objectMapper) {
        this.subscribeUseCase = subscribeUseCase;
        this.changePlanUseCase = changePlanUseCase;
        this.cancelUseCase = cancelUseCase;
        this.getSubscriptionUseCase = getSubscriptionUseCase;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> commandWords = args.getNonOptionArgs();
        if (commandWords.isEmpty()) {
            printUsage();
            return;
        }
        String command = commandWords.get(0);

        try {
            switch (command) {
                case "subscribe" -> {
                    Subscription subscription = subscribeUseCase.execute(
                            new SubscribeCommand(flag(args, "customer"), flag(args, "plan")));
                    print(SubscriptionResponse.fromDomain(subscription));
                }
                case "change-plan" -> {
                    ChangePlanResult result = changePlanUseCase.execute(
                            new ChangePlanCommand(flag(args, "id"), flag(args, "plan")));
                    print(ChangePlanResponse.fromResult(result.subscription(), result.proratedAmount()));
                }
                case "cancel" -> {
                    Subscription subscription = cancelUseCase.execute(new CancelCommand(flag(args, "id")));
                    print(SubscriptionResponse.fromDomain(subscription));
                }
                case "get" -> {
                    Subscription subscription =
                            getSubscriptionUseCase.execute(new GetSubscriptionQuery(flag(args, "id")));
                    print(SubscriptionResponse.fromDomain(subscription));
                }
                default -> {
                    System.err.println(
                            "Unknown command \"" + command + "\". Use one of: subscribe | change-plan | cancel | get");
                    System.exit(1);
                }
            }
        } catch (DomainError e) {
            // Same DomainError the HTTP handler catches — the CLI just presents it differently.
            System.err.println("Domain error [" + e.getCode() + "]: " + e.getMessage());
            System.exit(1);
        }
    }

    private String flag(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private void print(Object value) throws Exception {
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value));
    }

    private void printUsage() {
        System.out.println("Usage: orbit-cli <command> [--flag=value ...]");
        System.out.println("Commands: subscribe | change-plan | cancel | get");
    }
}
