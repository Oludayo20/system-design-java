package com.systemdesign.faas.runtime;

/**
 * The handler contract every function under {@code functions/} implements — shaped like AWS
 * Lambda's own handler signature {@code (event, context) -> response}.
 *
 * <p>Implementations depend ONLY on {@link LambdaEvent}, {@link LambdaContext}, and
 * {@link LambdaResponse} — never on Spring MVC types, spring-amqp types, or anything else in
 * {@code triggers/} or the rest of {@code runtime/}. That is what makes them portable units of
 * code instead of framework-coupled route handlers: the exact same class could be redeployed
 * behind a real AWS Lambda runtime without changing a line.
 */
@FunctionalInterface
public interface LambdaFunction {

    LambdaResponse handle(LambdaEvent event, LambdaContext context);
}
