-- Identical migration, run independently against shard0, shard1 and shard2
-- (each shard DataSource config points Flyway at this same directory - see
-- Shard0/1/2DataSourceConfig). Each shard keeps its own
-- flyway_schema_history table, so running this three times against three
-- different databases is expected and safe - it is NOT the same as running
-- it three times against one database.
--
-- 1:1 port of src/infrastructure/postgres/migrations/shard/1700000000001-init-shard-schema.ts

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE "users" (
  "id" uuid PRIMARY KEY,
  "email" varchar NOT NULL UNIQUE,
  "password_hash" varchar NOT NULL,
  "full_name" varchar NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE "wallets" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "user_id" uuid NOT NULL UNIQUE REFERENCES "users"("id") ON DELETE CASCADE,
  "balance_cents" integer NOT NULL DEFAULT 0,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now()
);

CREATE TYPE "ledger_entry_type_enum" AS ENUM ('CREDIT', 'DEBIT');
CREATE TABLE "wallet_ledger_entries" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "wallet_id" uuid NOT NULL REFERENCES "wallets"("id") ON DELETE CASCADE,
  "type" "ledger_entry_type_enum" NOT NULL,
  "amount_cents" integer NOT NULL,
  "reason" varchar NOT NULL,
  "reference_id" uuid,
  "created_at" timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX "IDX_wallet_ledger_entries_wallet_id" ON "wallet_ledger_entries" ("wallet_id");
