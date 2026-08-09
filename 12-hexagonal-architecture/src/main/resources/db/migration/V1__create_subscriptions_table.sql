CREATE TABLE subscriptions (
    id                     VARCHAR(36)   NOT NULL,
    customer_id            VARCHAR(100)  NOT NULL,
    plan_id                VARCHAR(20)   NOT NULL,
    current_period_start   TIMESTAMPTZ   NOT NULL,
    current_period_end     TIMESTAMPTZ   NOT NULL,
    cancel_at_period_end   BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ   NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_subscriptions PRIMARY KEY (id)
);

CREATE INDEX ix_subscriptions_customer_id ON subscriptions (customer_id);
