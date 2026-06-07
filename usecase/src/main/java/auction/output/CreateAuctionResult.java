package auction.output;

import domain.auction.AuctionStatus;
import java.time.Instant;
import java.util.UUID;

public record CreateAuctionResult(UUID id, AuctionStatus status, Instant createdAt) {}
