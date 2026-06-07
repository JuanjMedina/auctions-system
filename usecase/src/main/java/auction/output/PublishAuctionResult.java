package auction.output;

import domain.auction.AuctionStatus;
import java.util.UUID;

public record PublishAuctionResult(UUID id, AuctionStatus status) {}
