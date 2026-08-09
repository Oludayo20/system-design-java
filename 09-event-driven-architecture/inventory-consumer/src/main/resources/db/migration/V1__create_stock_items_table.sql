-- Owned exclusively by inventory-consumer. order-api, notification-consumer,
-- analytics-consumer, and loyalty-consumer never touch this database.
CREATE TABLE stock_items (
    sku         VARCHAR(50)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    quantity    INT          NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
