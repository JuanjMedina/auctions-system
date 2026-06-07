package auction.input;

import java.util.UUID;

public record PublishAuctionInput(UUID auctionId, UUID sellerId) {}
