CREATE TYPE "ordering"."ordering_order_status" AS ENUM('placed', 'cancelled');

CREATE TABLE "ordering"."orders" (
  "id" uuid NOT NULL DEFAULT uuid_generate_v4(),
  "user_id" uuid NOT NULL,
  "status" "ordering"."ordering_order_status" NOT NULL DEFAULT 'placed',
  "total" numeric(10,2) NOT NULL,
  "created_at" TIMESTAMP NOT NULL DEFAULT now(),
  "updated_at" TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT "pk_ordering_orders" PRIMARY KEY ("id")
);

CREATE INDEX "ix_ordering_orders_user" ON "ordering"."orders" ("user_id");

CREATE TABLE "ordering"."order_items" (
  "id" uuid NOT NULL DEFAULT uuid_generate_v4(),
  "order_id" uuid NOT NULL,
  "product_id" uuid NOT NULL,
  "product_name" varchar(200) NOT NULL,
  "unit_price" numeric(10,2) NOT NULL,
  "quantity" int NOT NULL,
  "line_total" numeric(10,2) NOT NULL,
  CONSTRAINT "pk_ordering_order_items" PRIMARY KEY ("id"),
  CONSTRAINT "fk_ordering_order_items_order" FOREIGN KEY ("order_id")
    REFERENCES "ordering"."orders" ("id") ON DELETE CASCADE
);

-- product_id/product_name/unit_price are a point-in-time snapshot copied out of Catalog at
-- order-placement time; deliberately no FK into catalog.products (order history must stay
-- correct even if a product is later renamed, repriced, or deleted).
CREATE INDEX "ix_ordering_order_items_order" ON "ordering"."order_items" ("order_id");
