-- Owned exclusively by order-api. No other app in this project reads or writes order-db.
CREATE TABLE orders (
    id            UUID PRIMARY KEY,
    customer_id   VARCHAR(100)   NOT NULL,
    total_amount  NUMERIC(10,2)  NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'placed',
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
    id          UUID PRIMARY KEY,
    order_id    UUID           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sku         VARCHAR(50)    NOT NULL,
    name        VARCHAR(200)   NOT NULL,
    quantity    INT            NOT NULL,
    unit_price  NUMERIC(10,2)  NOT NULL
);

CREATE INDEX ix_order_items_order ON order_items(order_id);
