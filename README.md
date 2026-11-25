# FinLab 🏦

**A study fintech that evolves one version at a time, from a single working bank account into a well-built system. In public.**

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![Version](https://img.shields.io/badge/version-v0.4-blue)

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

## What it does (v0.4)

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
mvn spring-boot:run
```

Then open **http://localhost:8081**.

> The port is set to `8081` in `src/main/resources/application.properties`. Remove that line to use Spring's default, `8080`.

## Project structure

```
finlab/
├── pom.xml                                   # dependencies: spring-boot-starter-web
└── src/main/
    ├── java/dev/adrianlopes/finlab/
    │   └── FinlabApplication.java            # startup, page, rules and error page
    └── resources/
        └── application.properties            # server port
```

| Piece | What it does |
|---|---|
| `@SpringBootApplication` + `main` | Starts Spring Boot and the embedded Tomcat server |
| `@Controller` | Handles the HTTP requests |
| `implements ErrorController` | Replaces Spring's default error page with a FinLab page |
| `balance` | The account balance (`BigDecimal`), kept in memory |
| `synchronized` | Only one request reads or changes the balance at a time |
| `requestId` + `processedRequests` | Each form carries a unique id; a form sent twice is applied only once |
| `GET /` | Returns the HTML page with the balance, a message and the form |
| `POST /deposit` | Applies the deposit rule, then redirects to `/` |
| `POST /withdraw` | Applies the withdrawal rule, then redirects to `/` |
| `GET /deposit`, `GET /withdraw` | Redirect to `/` (opening those URLs directly no longer shows an error) |
| `/error` | Error page with a link back to the account (404, 405, ...) |
| `toMoney` / `validate` / `formatMoney` | Parse, check and display amounts |

The redirect after each `POST` follows the *Post/Redirect/Get* pattern: refreshing the page doesn't submit the operation again.

## Versions

Each version starts from a problem found in the previous one. Newest first.

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
