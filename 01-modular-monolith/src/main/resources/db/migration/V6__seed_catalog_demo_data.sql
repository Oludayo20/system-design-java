INSERT INTO "catalog"."categories" ("name", "slug") VALUES
  ('Computers', 'computers'),
  ('Audio', 'audio'),
  ('Accessories', 'accessories');

INSERT INTO "catalog"."products" ("name", "description", "price", "stock", "category_id")
VALUES
  (
    'Laptop Pro 14"',
    '14-inch laptop with 32GB RAM and a 1TB SSD, built for developers.',
    '1899.00',
    25,
    (SELECT "id" FROM "catalog"."categories" WHERE "slug" = 'computers')
  ),
  (
    'Wireless Mouse',
    'Ergonomic wireless mouse with silent clicks and USB-C charging.',
    '39.99',
    200,
    (SELECT "id" FROM "catalog"."categories" WHERE "slug" = 'accessories')
  ),
  (
    'Mechanical Keyboard',
    'Hot-swappable mechanical keyboard with tactile brown switches.',
    '129.00',
    80,
    (SELECT "id" FROM "catalog"."categories" WHERE "slug" = 'accessories')
  ),
  (
    'Noise Cancelling Headphones',
    'Over-ear headphones with active noise cancellation and 30h battery life.',
    '249.50',
    60,
    (SELECT "id" FROM "catalog"."categories" WHERE "slug" = 'audio')
  ),
  (
    '27" 4K Monitor',
    '27-inch 4K IPS monitor with USB-C power delivery.',
    '449.00',
    40,
    (SELECT "id" FROM "catalog"."categories" WHERE "slug" = 'computers')
  );
