-- One app, one database, one schema. IDs are generated in the Application layer
-- (UUID.randomUUID(), see e.g. books/application/CreateBookUseCase.java) before the Data Access
-- layer ever sees the row, so no DB-side UUID default is needed here.

CREATE TABLE "books" (
  "id" uuid NOT NULL,
  "title" varchar(255) NOT NULL,
  "author" varchar(255) NOT NULL,
  "isbn" varchar(20) NOT NULL,
  "total_copies" int NOT NULL,
  "available_copies" int NOT NULL,
  CONSTRAINT "pk_books" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uq_books_isbn" ON "books" ("isbn");

CREATE TABLE "members" (
  "id" uuid NOT NULL,
  "name" varchar(255) NOT NULL,
  "email" varchar(255) NOT NULL,
  "membership_status" varchar(20) NOT NULL DEFAULT 'ACTIVE',
  CONSTRAINT "pk_members" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uq_members_email" ON "members" ("email");

CREATE TABLE "loans" (
  "id" uuid NOT NULL,
  "book_id" uuid NOT NULL,
  "member_id" uuid NOT NULL,
  "borrowed_at" TIMESTAMP NOT NULL,
  "due_at" TIMESTAMP NOT NULL,
  "returned_at" TIMESTAMP,
  CONSTRAINT "pk_loans" PRIMARY KEY ("id"),
  CONSTRAINT "fk_loans_book" FOREIGN KEY ("book_id") REFERENCES "books" ("id") ON DELETE RESTRICT,
  CONSTRAINT "fk_loans_member" FOREIGN KEY ("member_id") REFERENCES "members" ("id") ON DELETE RESTRICT
);

CREATE INDEX "ix_loans_member" ON "loans" ("member_id");
CREATE INDEX "ix_loans_book" ON "loans" ("book_id");
