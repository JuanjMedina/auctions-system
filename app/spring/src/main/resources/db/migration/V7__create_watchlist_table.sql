CREATE TABLE watchlist
(
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    auction_id UUID        NOT NULL REFERENCES auctions (id) ON DELETE CASCADE,
    added_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_watchlist_user_auction UNIQUE (user_id, auction_id)
);

CREATE INDEX idx_watchlist_user_id    ON watchlist (user_id);
CREATE INDEX idx_watchlist_auction_id ON watchlist (auction_id);
