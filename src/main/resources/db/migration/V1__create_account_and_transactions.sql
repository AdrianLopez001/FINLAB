-- The single FinLab account. Its balance can never go below zero.
CREATE TABLE account (
    id      BIGINT         PRIMARY KEY,
    balance NUMERIC(19, 2) NOT NULL CHECK (balance >= 0)
);

INSERT INTO account (id, balance) VALUES (1, 0);

-- One row per applied operation: the account statement.
-- request_id is UNIQUE, so the same form can never be stored twice.
CREATE TABLE transactions (
    id            BIGSERIAL      PRIMARY KEY,
    request_id    VARCHAR(64)    NOT NULL UNIQUE,
    type          VARCHAR(10)    NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAWAL')),
    amount        NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    balance_after NUMERIC(19, 2) NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL
);
