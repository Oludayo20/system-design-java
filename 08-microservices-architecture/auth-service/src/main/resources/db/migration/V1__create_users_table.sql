-- Lives only in auth-db. No other BookHive service has this connection string, so no other
-- service can ever query this table directly - catalog-service and order-service only ever
-- learn a user's identity from a verified JWT `sub` claim.
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
