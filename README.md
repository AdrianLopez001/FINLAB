# FinLab 🏦

**A study fintech that evolves one version at a time, from a single working bank account into a well-built system. In public.**

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791)
![Version](https://img.shields.io/badge/version-v0.6-blue)

---

## Why this project exists

Building four small projects in a month usually means learning the same basics four times.
FinLab does the opposite: **one project, improved continuously.**

Every version follows the same cycle:

1. **Review** the current code
2. **Identify** what can be improved
3. **Implement** the improvement the way a production system would
4. **Document** what changed and why

Each change brings new questions, and those become the next improvements. The git history is part of the project: every version is tagged, so you can check out any of them and see exactly where the code was at that point.

## What it does (v0.6)

A bank account in the browser:

- Shows the current **balance**
- **Deposit** an amount
- **Withdraw** an amount
- Keeps a **statement**: every operation with date and time, type, amount and the balance after it

Everything is stored in **PostgreSQL**, so the balance and the history survive restarts.

Every amount is validated before touching the balance:

| Condition | Result |
|---|---|
| Missing or not a number (`abc`, `NaN`, `Infinity`) | rejected: *"Enter a valid amount, like 10.50."* |
| Zero or negative | rejected: *"Amount must be greater than zero."* |
| More than 2 decimal places (`10.005`) | rejected: *"Amount must have at most 2 decimal places."* |
| Above R$ 1.000.000,00 per operation | rejected: *"Amount must be at most R$ 1.000.000,00."* |

Then:

| Action | Condition | Result |
|---|---|---|
| Deposit | valid amount | added to the balance |
| Withdraw | amount > balance | rejected: *"Insufficient balance."* |
| Withdraw | otherwise | subtracted from the balance |

Money is stored as `BigDecimal`, so values like `0.10` are exact, and it's always shown in Brazilian format (`R$ 1.234,56`).

## How to run

Requirements: **Java 21**, **Maven** and **Docker**.

```bash
docker compose up -d    # starts PostgreSQL (port 5432)
mvn spring-boot:run     # starts the app; Flyway creates the tables on the first run
mvn test                # runs the automated tests (starts its own PostgreSQL in Docker)
```

Then open **http://localhost:8081**. The full statement is at **http://localhost:8081/statement**.

> Port `8081` is set in `src/main/resources/application.properties`. The database connection can be changed with the `DB_URL`, `DB_USER` and `DB_PASSWORD` environment variables, and the Docker port with `DB_PORT`.

## Project structure

```
finlab/
├── pom.xml                                   # dependencies: web, thymeleaf, data-jpa, flyway, postgresql, test
├── docker-compose.yml                        # PostgreSQL 16 for local development
└── src/
    ├── main/
    │   ├── java/dev/adrianlopes/finlab/
    │   │   ├── FinlabApplication.java        # starts the app (nothing else)
    │   │   └── account/
    │   │       ├── AccountController.java    # HTTP: routes, messages, which page to show
    │   │       ├── AccountService.java       # business rules, one database transaction per operation
    │   │       ├── Account.java              # entity: the account row (balance)
    │   │       ├── Transaction.java          # entity: one row of the statement
    │   │       ├── TransactionType.java      # DEPOSIT or WITHDRAWAL
    │   │       ├── AccountRepository.java    # database access for the account (with row lock)
    │   │       ├── TransactionRepository.java # database access for the statement
    │   │       ├── TransactionView.java      # a transaction formatted for the page
    │   │       ├── Money.java                # text -> money -> text (parse, validate, format)
    │   │       ├── OperationException.java   # a refused operation, with the reason
    │   │       └── DuplicateOperationException.java  # the same form arrived twice
    │   └── resources/
    │       ├── application.properties        # port and database settings
    │       ├── db/migration/
    │       │   └── V1__create_account_and_transactions.sql  # tables, created by Flyway
    │       └── templates/
    │           ├── home.html                 # account page + recent transactions
    │           ├── statement.html            # full statement
    │           ├── fragments.html            # transactions table, shared by both pages
    │           └── error.html                # FinLab error page (404, 405, ...)
    └── test/java/dev/adrianlopes/finlab/
        ├── AccountFlowTest.java              # uses the app like a browser, against a real PostgreSQL
        └── account/MoneyTest.java            # unit tests for parsing and formatting
```

Each layer only talks to the one below it:

```
browser ──HTTP──> AccountController ──> AccountService ──> AccountRepository ──┐
                        │                     │            TransactionRepository ┴──> PostgreSQL
                   templates/*.html        Money (rules for amounts)
```

| Piece | What it does |
|---|---|
| `AccountController` | Receives the request, calls the service, puts the result message in a *flash attribute* and redirects |
| `AccountService` | Each operation runs in one `@Transactional` database transaction: lock the account row, check the form wasn't applied before, validate, update the balance, save the statement row |
| `Account` / `Transaction` | JPA entities: Java classes mapped to the `account` and `transactions` tables |
| `AccountRepository` / `TransactionRepository` | Spring Data interfaces; queries come from method names (`existsByRequestId`) or `@Query` |
| `findByIdForUpdate` | `SELECT ... FOR UPDATE`: the account row stays locked until the operation ends, so operations never overlap |
| `V1__create_account_and_transactions.sql` | Flyway migration: the schema is versioned SQL; Hibernate only validates it (`ddl-auto=validate`) |
| `Money` | Turns typed text into a valid `BigDecimal` and formats it as `R$ 1.234,56` |
| `home.html` / `statement.html` / `error.html` | The pages, rendered by Thymeleaf |

The redirect after each `POST` follows the *Post/Redirect/Get* pattern: refreshing the page doesn't submit the operation again.

## Versions

Each version starts from a problem found in the previous one. Newest first.

### [v0.6](https://github.com/AdrianLopez001/FINLAB/tree/v0.6) — PostgreSQL and transaction history

- **Problem:** the balance lived in memory, so restarting the app erased the money, and there was no record of what happened or when. `synchronized` also only protected one running copy of the app.
- **Solution:** PostgreSQL runs in Docker (`docker-compose.yml`), Flyway creates the tables, and Spring Data JPA reads and writes them. Every applied operation becomes a row in `transactions` (type, amount, balance after, date and time), shown on the home page and in `/statement`. Each operation is one database transaction that locks the account row (`SELECT ... FOR UPDATE`), which replaces `synchronized` and works across app instances. `request_id` is `UNIQUE`, so a form can't be stored twice. Tests run against a real PostgreSQL started by Testcontainers.

### [v0.5](https://github.com/AdrianLopez001/FINLAB/tree/v0.5) — Layers, templates and tests

- **Problem:** one class did everything: started the app, built the HTML as strings, validated, stored the balance and handled errors. Every change touched the same file, and nothing proved the rules still worked after a change. The message on screen was also global, so one user could see another user's message.
- **Solution:** the code is split into layers inside `account/` (controller → service → repository), money rules moved to `Money`, and the pages became Thymeleaf templates. Before moving anything, 20 black-box tests were written against v0.4 (they use the app like a browser); the same tests pass after the refactor. Messages are now per user (*flash attributes*), and a repeated form is ignored silently, so only the first result is shown.

### [v0.4](https://github.com/AdrianLopez001/FINLAB/tree/v0.4) — One click, one operation

- **Problem:** clicking *Deposit* (or *Withdraw*) twice very fast sent the same form twice, and the server applied it twice: R$ 10,00 became R$ 20,00. `synchronized` only put the two requests in line; it didn't know they were the same operation.
- **Solution:** every form carries a unique `requestId` (a UUID). The server remembers the ids it has applied, and a second copy of the same form is ignored with *"This operation was already processed."* (idempotency). In the browser, the buttons are also disabled right after the first click.

### [v0.3](https://github.com/AdrianLopez001/FINLAB/tree/v0.3) — Concurrent operations

- **Problem:** 200 simultaneous deposits of R$ 1,00 raised the balance by only R$ 181,00. Two requests could read the same balance at the same time and one overwrote the other's update (*lost update*).
- **Solution:** the methods that read or change the balance (`home`, `deposit`, `withdraw`) are `synchronized`, so requests touch it one at a time. Verified with 600 simultaneous deposits (none lost) and 10 simultaneous withdrawals of R$ 80,00 from R$ 100,00 (only one approved).

### [v0.2](https://github.com/AdrianLopez001/FINLAB/tree/v0.2) — Exact money and validation

- **Problem:** money was a `double`, so R$ 0,30 − 0,10 − 0,10 became 0,0999… and a R$ 0,10 balance refused a R$ 0,10 withdrawal. `NaN` and `Infinity` passed the `amount <= 0` check and broke the balance for good. Opening `/deposit` in the browser showed Spring's default error page.
- **Solution:** money is a `BigDecimal`. Every amount is parsed and validated on the server (a number, greater than zero, at most 2 decimals, at most R$ 1.000.000,00). `GET /deposit` and `GET /withdraw` redirect home, and errors show a FinLab page.

### [v0.1](https://github.com/AdrianLopez001/FINLAB/tree/v0.1) — First working version

- Balance, deposit and withdrawal in the browser, in a single class.

## Follow along

Every version comes with a write-up on LinkedIn explaining what changed and what I learned along the way.
