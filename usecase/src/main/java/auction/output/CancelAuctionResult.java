package auction.output;

import domain.auction.AuctionStatus;
import java.util.UUID;

public record CancelAuctionResult(UUID auctionId, AuctionStatus status, int releasedBids) {}
