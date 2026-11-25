# FinLab 🏦

**A study fintech that evolves one version at a time, from a single working bank account into a well-built system. In public.**

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![Version](https://img.shields.io/badge/version-v0.5-blue)

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

## What it does (v0.5)

A bank account in the browser:

- Shows the current **balance**
- **Deposit** an amount
- **Withdraw** an amount

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

Requirements: **Java 21** and **Maven**.

```bash
mvn spring-boot:run     # starts the app
mvn test                # runs the automated tests
```

Then open **http://localhost:8081**.

> The port is set to `8081` in `src/main/resources/application.properties`. Remove that line to use Spring's default, `8080`.

## Project structure

```
finlab/
├── pom.xml                                   # dependencies: web, thymeleaf, test
└── src/
    ├── main/
    │   ├── java/dev/adrianlopes/finlab/
    │   │   ├── FinlabApplication.java        # starts the app (nothing else)
    │   │   └── account/
    │   │       ├── AccountController.java    # HTTP: routes, messages, which page to show
    │   │       ├── AccountService.java       # business rules: validation, deposit, withdraw
    │   │       ├── AccountRepository.java    # where the data lives (in memory for now)
    │   │       ├── Money.java                # text -> money -> text (parse, validate, format)
    │   │       ├── OperationException.java   # a refused operation, with the reason
    │   │       └── DuplicateOperationException.java  # the same form arrived twice
    │   └── resources/
    │       ├── application.properties        # server port
    │       └── templates/
    │           ├── home.html                 # account page (Thymeleaf)
    │           └── error.html                # FinLab error page (404, 405, ...)
    └── test/java/dev/adrianlopes/finlab/
        ├── AccountFlowTest.java              # uses the app like a browser (HTTP + cookies)
        └── account/MoneyTest.java            # unit tests for parsing and formatting
```

Each layer only talks to the one below it:

```
browser ──HTTP──> AccountController ──> AccountService ──> AccountRepository
                        │                     │
                   templates/*.html        Money (rules for amounts)
```

| Piece | What it does |
|---|---|
| `AccountController` | Receives the request, calls the service, puts the result message in a *flash attribute* and redirects |
| `AccountService` | Checks the request and the amount, applies the operation; `synchronized`, so one operation at a time |
| `AccountRepository` | Keeps the balance and the ids of forms already applied |
| `Money` | Turns typed text into a valid `BigDecimal` and formats it as `R$ 1.234,56` |
| `OperationException` | Thrown when an operation is refused; its message is what the user sees |
| `home.html` / `error.html` | The pages, as real HTML files rendered by Thymeleaf instead of strings in Java |

The redirect after each `POST` follows the *Post/Redirect/Get* pattern: refreshing the page doesn't submit the operation again.

## Versions

Each version starts from a problem found in the previous one. Newest first.

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
