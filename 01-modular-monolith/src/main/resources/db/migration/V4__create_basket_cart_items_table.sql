CREATE TABLE "basket"."cart_items" (
  "id" uuid NOT NULL DEFAULT uuid_generate_v4(),
  "user_id" uuid NOT NULL,
  "product_id" uuid NOT NULL,
  "quantity" int NOT NULL,
  "added_at" TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT "pk_basket_cart_items" PRIMARY KEY ("id"),
  CONSTRAINT "uq_cart_items_user_product" UNIQUE ("user_id", "product_id")
);

-- No foreign key to catalog.products or identity.users on purpose: Basket never joins
-- across schemas, it resolves product data through CatalogService at read time instead.
CREATE INDEX "ix_basket_cart_items_user" ON "basket"."cart_items" ("user_id");
