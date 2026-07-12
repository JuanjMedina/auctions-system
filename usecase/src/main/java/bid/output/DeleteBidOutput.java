package bid.output;

import domain.bid.BidStatus;
import java.util.UUID;

public record DeleteBidOutput(UUID bidId, UUID auctionId, BidStatus status) {}
