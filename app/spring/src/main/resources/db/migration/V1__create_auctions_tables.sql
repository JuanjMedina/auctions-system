CREATE TABLE auctions (
    id            UUID PRIMARY KEY,
    title         VARCHAR(255)   NOT NULL,
    description   TEXT,
    starting_price DECIMAL(19, 4) NOT NULL,
    start_time    TIMESTAMPTZ    NOT NULL,
    end_time      TIMESTAMPTZ    NOT NULL,
    status        VARCHAR(50)    NOT NULL
);

CREATE TABLE auction_bids (
    auction_id UUID           NOT NULL REFERENCES auctions (id),
    bidder_id  VARCHAR(255)   NOT NULL,
    amount     DECIMAL(19, 4) NOT NULL,
    placed_at  TIMESTAMPTZ    NOT NULL
);
