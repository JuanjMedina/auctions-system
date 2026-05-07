CREATE TABLE bids (
    id              UUID          PRIMARY KEY,
    auction_id      UUID          NOT NULL REFERENCES auctions (id),
    bidder_id       UUID          NOT NULL,
    amount          DECIMAL(19,4) NOT NULL,
    is_auto_bid     BOOLEAN       NOT NULL DEFAULT FALSE,
    max_auto_amount DECIMAL(19,4),
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_bids_amount CHECK (amount > 0),
    CONSTRAINT ck_bids_auto CHECK (
        (is_auto_bid = FALSE AND max_auto_amount IS NULL) OR
        (is_auto_bid = TRUE  AND max_auto_amount IS NOT NULL AND max_auto_amount >= amount)
    )
);

CREATE INDEX idx_bids_auction         ON bids (auction_id);
CREATE INDEX idx_bids_bidder          ON bids (bidder_id);
CREATE INDEX idx_bids_auction_amount  ON bids (auction_id, amount DESC);
CREATE INDEX idx_bids_created         ON bids (created_at DESC);
