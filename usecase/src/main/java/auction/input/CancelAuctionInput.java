package auction.input;

import java.util.UUID;

public record CancelAuctionInput(UUID auctionId, UUID sellerId) {}
