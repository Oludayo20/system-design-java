package com.systemdesign.orbit.core.domain;

/** Business rule 4: one active subscription per customer. */
public class CustomerAlreadySubscribedError extends DomainError {
    public CustomerAlreadySubscribedError(String customerId) {
        super("Customer \"" + customerId + "\" already has an active subscription.", "CUSTOMER_ALREADY_SUBSCRIBED");
    }
}
