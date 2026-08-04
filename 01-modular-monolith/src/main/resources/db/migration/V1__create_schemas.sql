-- One PostgreSQL instance, one database -- but schema-per-module (the "stronger boundary"
-- option described in the design doc). Each feature module owns exactly one schema and no
-- module is granted cross-schema foreign keys into another module's tables.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE SCHEMA IF NOT EXISTS "identity";
CREATE SCHEMA IF NOT EXISTS "catalog";
CREATE SCHEMA IF NOT EXISTS "basket";
CREATE SCHEMA IF NOT EXISTS "ordering";
