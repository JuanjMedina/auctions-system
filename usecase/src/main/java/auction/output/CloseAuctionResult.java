package auction.output;

import domain.auction.AuctionStatus;
import java.util.UUID;

public record CloseAuctionResult(UUID auctionId, AuctionStatus status, UUID winnerId) {}
