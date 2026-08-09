-- One shared database, no schema-per-module split: every table below lives in the default
-- "public" schema, and later migrations add real foreign keys straight across module lines
-- (comments -> posts, comments -> users, notifications -> users) because in this project there
-- is no boundary stopping them.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  email varchar(255) NOT NULL,
  password_hash varchar(255) NOT NULL,
  display_name varchar(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uq_users_email ON users (email);
