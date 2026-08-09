-- Lives only in catalog-db. order-service NEVER queries this table - it asks catalog-service
-- for price/stock over HTTP (POST /books/:id/reserve) and gets back a plain JSON response.
CREATE TABLE books (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    price_cents INTEGER NOT NULL,
    stock INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
