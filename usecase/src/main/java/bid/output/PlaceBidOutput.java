package bid.output;

import domain.bid.BidStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlaceBidOutput(
    UUID bidId, UUID auctionId, BigDecimal amount, BidStatus status, Instant createdAt) {}
