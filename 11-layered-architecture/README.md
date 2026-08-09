# Riverside Library — N-Tier / Layered Architecture (Java / Spring Boot)

Project 11 of the system design series, ported from the original NestJS/TypeScript implementation
to Java/Spring Boot. This one demonstrates **Layered Architecture (N-Tier)**: organizing code
*inside a single application* into layers with a strict, one-directional dependency flow, rather
than splitting the application into separate deployable services.

## Concept, in my own words

Picture a codebase sliced into horizontal bands stacked on top of each other, where a band is only
ever allowed to call the band directly beneath it:

```
Presentation -> Application -> Domain -> Data Access -> Database
```

- **Presentation** (`*Controller` classes + request/response DTOs) speaks HTTP and nothing else —
  routes, status codes, request bodies — and never reaches past the Application layer to touch a
  repository or a database row itself.
- **Application** (`*UseCase` classes, one per user action: "borrow a book", "return a book") is
  the orchestrator. It fetches this, asks that, saves this — but it owns no business rules of its
  own. It hands the decision to the Domain layer and executes whatever it's told.
- **Domain** (plain classes in every `domain` package) holds the actual business rules, and it's
  the only layer that matters for correctness. It's deliberately framework-agnostic: no Spring
  annotations, no JPA, no HTTP status codes. It has no idea it's running inside a web server.
- **Data Access** (`infrastructure` packages) is the only layer allowed to import an ORM or talk to
  a real database. It implements repository *port interfaces* that the Domain/Application layers
  depend on, so the rest of the app never has to care whether rows live in Postgres, SQLite, or an
  in-memory fake.
- **Database** — PostgreSQL. Just storage.

I like the restaurant picture for this: the **diner** is the Presentation layer — they place an
order and expect a plate back, nothing more. The **waiter** is the Application layer — they carry
the order to the kitchen and the plate back out, but they never decide what "medium rare" means or
whether the kitchen has run out of salmon that's the kitchen's call, not theirs. The **chef** is
the Domain layer — every actual cooking decision (recipes, substitutions, what counts as "done")
belongs to the chef, and the chef doesn't care whether the ticket came from a phone app or a
handwritten notepad. The **line cooks running to the walk-in** are the Data Access layer — they're
the only ones with a key to the storage room. The **walk-in itself** is the Database. If a diner
tried to walk straight into the kitchen and grab ingredients off the shelf, the whole model breaks
— and that shortcut is exactly what this architecture makes structurally impossible: the diner's
code literally has no reference to anything inside the kitchen.

## This is not microservices

Hearing "layers" can conjure images of separate services chattering over a network — this is the
opposite. Riverside Library is **one Spring Boot process, one `mvn package`, one Docker image, one
deployment**. There is no HTTP call, no message queue, no network hop between Presentation and
Domain — it's one Java method calling another inside the same JVM, the same request thread. The
boundaries here are *compile-time* discipline: package structure, port interfaces, and the rule
that an import only ever points "down" — enforced by convention and code review, not by a network
socket. You get the separation of concerns without any of the operational cost of distributed
systems (service discovery, partial failure, distributed transactions). Microservices split a
system by *business capability* across processes; layered architecture organizes *one* business
capability's codebase internally. The two solve different problems and compose fine — a
microservice can (and often should) be internally layered exactly like this.

## Domain model

- **Book** — `title`, `author`, `isbn`, `totalCopies`, `availableCopies`
- **Member** — `name`, `email`, `membershipStatus`
- **Loan** — `bookId`, `memberId`, `borrowedAt`, `dueAt`, `returnedAt`

## The 4 business rules (all live in `loans/domain/`)

1. A member cannot borrow a book if `availableCopies == 0`.
2. A member cannot have more than 3 active (unreturned) loans at once.
3. A member with any overdue loan (past `dueAt`, unreturned) cannot borrow further books until
   they return it.
4. Returning a book increments `availableCopies` and sets `returnedAt`.

All four are implemented as plain Java in
[`src/main/java/com/systemdesign/library/loans/domain/LoanEligibilityRules.java`](./src/main/java/com/systemdesign/library/loans/domain/LoanEligibilityRules.java)
(rules 1–3) and
[`loans/domain/Loan.java`](./src/main/java/com/systemdesign/library/loans/domain/Loan.java) /
[`books/domain/Book.java`](./src/main/java/com/systemdesign/library/books/domain/Book.java) (rule
4). None of those files import `org.springframework.*` or `jakarta.persistence.*` — check for
yourself:

```bash
grep -rln -E "org\.springframework|jakarta\.persistence" src/main/java/com/systemdesign/library/*/domain/
# (no output)
```

## One module's package structure — `loans/`

```text
src/main/java/com/systemdesign/library/loans/
├── presentation/
│   ├── LoanController.java          # HTTP only: routes, path vars, calls into application/
│   └── dto/
│       ├── BorrowBookRequest.java   # request shape + jakarta.validation rules
│       └── LoanResponse.java        # response shape for Swagger
├── application/
│   ├── BorrowBookUseCase.java       # orchestrates: fetch book+member, ask domain, save
│   ├── ReturnBookUseCase.java       # orchestrates: fetch loan, ask domain, free up stock
│   └── ListMemberLoansUseCase.java
├── domain/
│   ├── Loan.java                       # pure class: isOverdue(), markReturned()
│   ├── LoanEligibilityRules.java       # pure class: the 3 borrowing rules
│   ├── BookUnavailableException.java   # plain RuntimeException subclasses
│   ├── MaxActiveLoansExceededException.java
│   ├── OverdueLoanExistsException.java
│   ├── LoanAlreadyReturnedException.java
│   └── LoanRepositoryPort.java         # interface — no JPA/Spring Data types
└── infrastructure/
    ├── LoanJpaEntity.java              # @Entity — the only loan-shaped class JPA knows about
    ├── LoanJpaRepository.java          # Spring Data JpaRepository (package-private)
    └── LoanRepositoryAdapter.java      # implements LoanRepositoryPort against Postgres
```

`books/` and `members/` mirror the same four sub-packages.

## Walkthrough: `POST /loans` through all 4 layers

1. **Presentation** —
   [`loans/presentation/LoanController.java`](./src/main/java/com/systemdesign/library/loans/presentation/LoanController.java)'s
   `borrow()` method receives the HTTP request; `@Valid` has already validated the body against
   [`BorrowBookRequest`](./src/main/java/com/systemdesign/library/loans/presentation/dto/BorrowBookRequest.java)
   (`bookId`, `memberId` must be non-null UUIDs). The controller does nothing else — it calls
   `borrowBookUseCase.execute(request)` and wraps whatever comes back in a `201`.

2. **Application** —
   [`loans/application/BorrowBookUseCase.java`](./src/main/java/com/systemdesign/library/loans/application/BorrowBookUseCase.java)'s
   `execute()` orchestrates the steps: load the `Book` via `BookRepositoryPort`, load the `Member`
   via `MemberRepositoryPort`, load the member's active loans via
   `LoanRepositoryPort.findActiveByMemberId()`. Then — the important part — it does **not** itself
   decide whether borrowing is allowed. It calls
   `LoanEligibilityRules.assertCanBorrow(book, activeLoans, Instant.now())` and lets the Domain
   layer throw if a rule is violated.

3. **Domain** —
   [`loans/domain/LoanEligibilityRules.java`](./src/main/java/com/systemdesign/library/loans/domain/LoanEligibilityRules.java)'s
   `assertCanBorrow()` runs the 3 borrowing checks in order (availability, active-loan cap,
   overdue block) against the plain `Book` and `List<Loan>` it was handed. If all 3 pass,
   `book.borrowOneCopy()` (also domain, in
   [`books/domain/Book.java`](./src/main/java/com/systemdesign/library/books/domain/Book.java))
   decrements `availableCopies`. None of this code has ever heard of Postgres or HTTP.

4. **Data Access** — back in the use case, `bookRepository.save(book)` and
   `loanRepository.save(loan)` are called against the *ports* (interfaces). At runtime Spring's
   component scan has wired those ports to
   [`BookRepositoryAdapter`](./src/main/java/com/systemdesign/library/books/infrastructure/BookRepositoryAdapter.java)
   and
   [`LoanRepositoryAdapter`](./src/main/java/com/systemdesign/library/loans/infrastructure/LoanRepositoryAdapter.java)
   (the only Spring bean implementing each port), which translate the plain domain objects into
   `BookJpaEntity`/`LoanJpaEntity` rows and issue the actual SQL against **Postgres**.

If any rule fails, the Domain layer throws a plain `RuntimeException` subclass
(`BookUnavailableException`, `MaxActiveLoansExceededException`, `OverdueLoanExistsException`) —
the Application layer catches those specific types and re-throws them as
`ResponseStatusException(HttpStatus.CONFLICT, message)`, which is the only place in this flow
where HTTP concepts and domain concepts touch.

## Run it

> **Hosting & deployment:** See [HOSTING.md](./HOSTING.md) for Docker setup, platforms, and
> production notes.

```bash
cp .env.example .env
docker compose up --build
```

Swagger UI: `http://localhost:3011/docs`

Or run the API on the host with infra in Docker:

```bash
cp .env.example .env
set -a && source .env && set +a
docker compose up -d postgres
mvn spring-boot:run
```

### Try it

This walkthrough exercises all 4 business rules against a fresh database. Copy the returned `id`
values as you go — env vars make the later commands copy-pasteable.

```bash
BASE=http://localhost:3011

# 1. Catalog a book with only 1 copy
BOOK_ID=$(curl -s -X POST $BASE/books -H 'Content-Type: application/json' \
  -d '{"title":"Clean Architecture","author":"Robert C. Martin","isbn":"9780134494166","totalCopies":1}' \
  | jq -r .id)

# 2. Register a member
MEMBER_ID=$(curl -s -X POST $BASE/members -H 'Content-Type: application/json' \
  -d '{"name":"Ada Lovelace","email":"ada@example.com"}' \
  | jq -r .id)

# 3. Borrow succeeds (1 available copy, 0 active loans, no overdue loans)
curl -s -X POST $BASE/loans -H 'Content-Type: application/json' \
  -d "{\"bookId\":\"$BOOK_ID\",\"memberId\":\"$MEMBER_ID\"}" | jq .
# -> 201, availableCopies on the book is now 0

# 4. Rule 1: borrowing the SAME book again is rejected — 0 copies available
curl -s -X POST $BASE/loans -H 'Content-Type: application/json' \
  -d "{\"bookId\":\"$BOOK_ID\",\"memberId\":\"$MEMBER_ID\"}" | jq .
# -> 409 {"detail":"\"Clean Architecture\" has no available copies right now.", ...}

# 5. Catalog 2 more single-copy books and borrow both. The member (who already had 1 active
#    loan from step 3) now reaches the 3-active-loan cap.
BID1=$(curl -s -X POST $BASE/books -H 'Content-Type: application/json' \
  -d '{"title":"Book 1","author":"Author 1","isbn":"0000000001","totalCopies":1}' | jq -r .id)
curl -s -X POST $BASE/loans -H 'Content-Type: application/json' \
  -d "{\"bookId\":\"$BID1\",\"memberId\":\"$MEMBER_ID\"}" > /dev/null

BID2=$(curl -s -X POST $BASE/books -H 'Content-Type: application/json' \
  -d '{"title":"Book 2","author":"Author 2","isbn":"0000000002","totalCopies":1}' | jq -r .id)
curl -s -X POST $BASE/loans -H 'Content-Type: application/json' \
  -d "{\"bookId\":\"$BID2\",\"memberId\":\"$MEMBER_ID\"}" > /dev/null

# 6. Rule 2: a 4th book is rejected by the active-loan cap (member now has exactly 3 active loans)
BID4=$(curl -s -X POST $BASE/books -H 'Content-Type: application/json' \
  -d '{"title":"Book 4","author":"Author 4","isbn":"0000000004","totalCopies":1}' | jq -r .id)
curl -s -X POST $BASE/loans -H 'Content-Type: application/json' \
  -d "{\"bookId\":\"$BID4\",\"memberId\":\"$MEMBER_ID\"}" | jq .
# -> 409 {"detail":"Member already has 3 active loan(s) (limit is 3).", ...}

# 7. Rule 4: return one of the active loans — frees up capacity
LOAN_ID=$(curl -s $BASE/members/$MEMBER_ID/loans | jq -r '.[0].id')
RET_BOOK_ID=$(curl -s $BASE/members/$MEMBER_ID/loans | jq -r '.[0].bookId')
curl -s -X POST $BASE/loans/$LOAN_ID/return | jq .
# -> 200, returnedAt is now set

curl -s $BASE/books/$RET_BOOK_ID | jq .availableCopies
# -> 1 (was 0) — returning a book increments availableCopies

# Returning an already-returned loan is rejected too:
curl -s -X POST $BASE/loans/$LOAN_ID/return | jq .
# -> 409 {"detail":"Loan ... has already been returned.", ...}

# Borrowing is possible again now that the member has fewer than 3 active loans
curl -s -X POST $BASE/loans -H 'Content-Type: application/json' \
  -d "{\"bookId\":\"$BID4\",\"memberId\":\"$MEMBER_ID\"}" | jq .
# -> 201
```

This exact sequence was run against a live `docker compose up` stack while validating this
project — every status code and message above is copied from a real response, not hand-written.
Error responses are Spring's RFC 7807 `ProblemDetail` bodies (`spring.mvc.problemdetails.enabled`
in `application.yml`); the domain error message lands in the `detail` field, which is the Java
equivalent of the `message` field in the NestJS original.

Rule 3 (overdue block) is time-based, so the fastest, most reliable place to see it enforced is
the pure unit test — `mvn test` runs it in milliseconds with a loan whose `dueAt` is constructed
in the past, no waiting required. The Docker walkthrough above shows rules 1, 2, and 4 live end to
end; rule 3 is exercised the same code path inside `BorrowBookUseCase` (any overdue, unreturned
loan blocks the same borrow flow checked in steps 4–5 above).

## Tests

```bash
mvn test
```

This is the actual payoff of the layering: business-rule tests need **no database, no HTTP
server, and no `@SpringBootTest`** — they `new Book(...)` / `new Loan(...)` directly and assert.
See:

- [`src/test/java/.../loans/domain/LoanEligibilityRulesTest.java`](./src/test/java/com/systemdesign/library/loans/domain/LoanEligibilityRulesTest.java) — all 3 borrowing rules
- [`src/test/java/.../loans/domain/LoanTest.java`](./src/test/java/com/systemdesign/library/loans/domain/LoanTest.java) — overdue detection, return rule
- [`src/test/java/.../books/domain/BookTest.java`](./src/test/java/com/systemdesign/library/books/domain/BookTest.java) — copy-count math
- [`src/test/java/.../members/domain/MemberTest.java`](./src/test/java/com/systemdesign/library/members/domain/MemberTest.java)

20 tests, all pure, run in well under a second even on a cold JVM start — compare that to spinning
up Postgres and a Spring application context just to check "does borrowing a 4th book fail."

## When this becomes overkill

Four explicit layers and a repository-port-interface-per-entity is a lot of ceremony for a small
CRUD app. If your endpoints are really just "validate input, `INSERT`/`SELECT` a row, return it"
with no real business rules to protect, splitting Presentation/Application/Domain into separate
packages and interfaces mostly adds indirection you have to jump through on every change, without
buying you anything — there's no complex decision logic worth isolating and unit-testing in
milliseconds. This pattern earns its keep specifically when a domain has non-trivial rules (like
the 4 borrowing rules here) that you want to test in isolation, evolve independently of the
database schema, and keep enforceable no matter which controller or background job ends up calling
into them. For a single `products` table with no business logic beyond "must have a price," a
two-layer controller-plus-repository split is enough — don't build the ceremony until the rules
show up that justify it.

## Related projects

| Project | Relationship |
|---|---|
| `01-modular-monolith` | Splits a codebase into *feature modules* (catalog, basket, ordering); this project splits *one* feature's codebase into *horizontal layers*. The two are complementary — 01's modules could each be internally layered like this. |
| `05-resilience` | A different cross-cutting concern (retries/circuit breakers) that would slot into the Data Access or an outbound-adapter layer of a system like this one. |
