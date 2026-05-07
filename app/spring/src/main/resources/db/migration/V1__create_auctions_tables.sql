CREATE TABLE auctions (
    id                UUID          PRIMARY KEY,
    seller_id         UUID          NOT NULL,
    category_id       UUID          NOT NULL,
    title             VARCHAR(200)  NOT NULL,
    description       TEXT,
    starting_price    DECIMAL(19,4) NOT NULL,
    reserve_price     DECIMAL(19,4),
    current_price     DECIMAL(19,4) NOT NULL,
    current_winner_id UUID,
    status            VARCHAR(20)   NOT NULL,
    auto_extend       BOOLEAN       NOT NULL DEFAULT FALSE,
    extend_minutes    INT           NOT NULL DEFAULT 5,
    starts_at         TIMESTAMPTZ   NOT NULL,
    ends_at           TIMESTAMPTZ   NOT NULL,
    closed_at         TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version           BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE auction_images (
    id            UUID         PRIMARY KEY,
    auction_id    UUID         NOT NULL REFERENCES auctions (id),
    url           VARCHAR(500) NOT NULL,
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_auctions_seller     ON auctions (seller_id);
CREATE INDEX idx_auctions_status     ON auctions (status);
CREATE INDEX idx_auctions_active_ends
    ON auctions (ends_at ASC)
    WHERE status IN ('ACTIVE', 'EXTENDED');
