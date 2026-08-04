-- Runs against the PRIMARY database only. Creates the Marketplace, Order and
-- user-directory schema (see doc.md: "shard only Users/Wallet by userId;
-- Marketplace/Orders/Notifications stay on a single well-indexed primary").
-- Seeds a small product catalog so GET /marketplace/products has data to
-- cache-aside from Redis on first run.
--
-- 1:1 port of src/infrastructure/postgres/migrations/primary/1700000000000-init-primary-schema.ts

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE "categories" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "name" varchar NOT NULL UNIQUE,
  "slug" varchar NOT NULL UNIQUE,
  "created_at" timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE "products" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "name" varchar NOT NULL,
  "description" text,
  "price_cents" integer NOT NULL,
  "stock" integer NOT NULL DEFAULT 0,
  "image_url" varchar,
  "category_id" uuid NOT NULL REFERENCES "categories"("id"),
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX "IDX_products_category_id" ON "products" ("category_id");

CREATE TYPE "order_status_enum" AS ENUM ('PENDING', 'SETTLED', 'CANCELLED');
CREATE TABLE "orders" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "user_id" uuid NOT NULL,
  "status" "order_status_enum" NOT NULL DEFAULT 'PENDING',
  "total_cents" integer NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX "IDX_orders_user_id" ON "orders" ("user_id");

CREATE TABLE "order_items" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "order_id" uuid NOT NULL REFERENCES "orders"("id") ON DELETE CASCADE,
  "product_id" uuid NOT NULL,
  "product_name" varchar NOT NULL,
  "quantity" integer NOT NULL,
  "unit_price_cents" integer NOT NULL
);
CREATE INDEX "IDX_order_items_order_id" ON "order_items" ("order_id");

-- Email -> {userId, shardIndex} directory. See UserDirectory entity for the
-- full rationale on why this table exists and its consistency tradeoffs.
CREATE TABLE "user_directory" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "email" varchar NOT NULL UNIQUE,
  "user_id" uuid NOT NULL UNIQUE,
  "shard_index" integer NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT now()
);

-- Seed catalog.
INSERT INTO "categories" ("id", "name", "slug") VALUES
  (gen_random_uuid(), 'Electronics', 'electronics');
INSERT INTO "categories" ("id", "name", "slug") VALUES
  (gen_random_uuid(), 'Fashion', 'fashion');
INSERT INTO "categories" ("id", "name", "slug") VALUES
  (gen_random_uuid(), 'Home & Living', 'home-living');

INSERT INTO "products" ("name", "description", "price_cents", "stock", "category_id")
SELECT 'Wireless Earbuds', 'Bluetooth 5.3 earbuds with charging case', 1599900, 50, "id" FROM "categories" WHERE "slug" = 'electronics'
UNION ALL
SELECT 'Power Bank 20000mAh', 'Fast-charging portable power bank', 899900, 100, "id" FROM "categories" WHERE "slug" = 'electronics'
UNION ALL
SELECT 'Ankara Print Shirt', 'Handmade Ankara print short-sleeve shirt', 1250000, 75, "id" FROM "categories" WHERE "slug" = 'fashion'
UNION ALL
SELECT 'Leather Sandals', 'Genuine leather unisex sandals', 950000, 60, "id" FROM "categories" WHERE "slug" = 'fashion'
UNION ALL
SELECT 'Non-stick Cookware Set', '6-piece non-stick pot and pan set', 4500000, 30, "id" FROM "categories" WHERE "slug" = 'home-living'
UNION ALL
SELECT 'Standing Fan', '18-inch rechargeable standing fan', 3200000, 40, "id" FROM "categories" WHERE "slug" = 'home-living';
