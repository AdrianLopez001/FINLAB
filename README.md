# FinLab 🏦

**A study fintech that evolves one version at a time, from a single working bank account into a well-built system. In public.**

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![Version](https://img.shields.io/badge/version-v0.1-blue)

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

## What it does (v0.1)

A bank account in the browser:

- Shows the current **balance**
- **Deposit** an amount
- **Withdraw** an amount

Rules:

| Action | Condition | Result |
|---|---|---|
| Deposit | amount ≤ 0 | rejected: *"Amount must be greater than zero."* |
| Deposit | amount > 0 | added to the balance |
| Withdraw | amount ≤ 0 | rejected: *"Amount must be greater than zero."* |
| Withdraw | amount > balance | rejected: *"Insufficient balance."* |
| Withdraw | otherwise | subtracted from the balance |

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
    │   └── FinlabApplication.java            # startup, page and rules
    └── resources/
        └── application.properties            # server port
```

| Piece | What it does |
|---|---|
| `@SpringBootApplication` + `main` | Starts Spring Boot and the embedded Tomcat server |
| `@Controller` | Handles the HTTP requests |
| `balance` | The account balance, kept in memory |
| `GET /` | Returns the HTML page with the balance, a message and the form |
| `POST /deposit` | Applies the deposit rule, then redirects to `/` |
| `POST /withdraw` | Applies the withdrawal rule, then redirects to `/` |

The redirect after each `POST` follows the *Post/Redirect/Get* pattern: refreshing the page doesn't submit the operation again.

## Versions

| Version | What changed |
|---|---|
| [v0.1](https://github.com/AdrianLopez001/FINLAB/tree/v0.1) | First working version: balance, deposit and withdrawal in the browser |

## Follow along

Every version comes with a write-up on LinkedIn explaining what changed and what I learned along the way.
