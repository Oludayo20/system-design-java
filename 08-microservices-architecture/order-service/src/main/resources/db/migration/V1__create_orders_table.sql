-- Lives only in order-db. `book_id` here is just a UUID string this table stores for
-- reference - order-db has no foreign key into catalog-db (it can't; they're different
-- Postgres instances with different credentials) and no join is ever possible or attempted.
-- Price is captured at order time (unit_price_cents) rather than looked up again later.
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    book_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price_cents INTEGER NOT NULL,
    total_cents INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'confirmed',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
