package bid.input;

import java.util.UUID;

public record DeleteBidInput(UUID bidId, UUID auctionId, UUID bidderId) {}
