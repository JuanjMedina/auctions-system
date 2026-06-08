package auction.output;

import domain.auction.AuctionStatus;
import java.util.UUID;

public record ActivateAuctionResult(UUID id, AuctionStatus status) {}
