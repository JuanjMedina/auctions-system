CREATE TABLE wallets
(
    id               UUID          PRIMARY KEY,
    user_id          UUID          NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    balance          DECIMAL(19,4) NOT NULL DEFAULT 0,
    reserved_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    currency         VARCHAR(3)    NOT NULL DEFAULT 'USD',
    version          BIGINT        NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_wallets_balance          CHECK (balance >= 0),
    CONSTRAINT ck_wallets_reserved_balance CHECK (reserved_balance >= 0),
    CONSTRAINT ck_wallets_reserved_lte_balance CHECK (reserved_balance <= balance)
);

CREATE TABLE wallet_transactions
(
    id            UUID          PRIMARY KEY,
    wallet_id     UUID          NOT NULL REFERENCES wallets (id) ON DELETE CASCADE,
    reference_id  UUID,
    type          VARCHAR(30)   NOT NULL,
    amount        DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    description   VARCHAR(255),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_wallet_tx_amount CHECK (amount > 0)
);

CREATE INDEX idx_wallets_user_id        ON wallets (user_id);
CREATE INDEX idx_wallet_tx_wallet_id    ON wallet_transactions (wallet_id);
CREATE INDEX idx_wallet_tx_reference_id ON wallet_transactions (reference_id);
CREATE INDEX idx_wallet_tx_created      ON wallet_transactions (created_at DESC);
