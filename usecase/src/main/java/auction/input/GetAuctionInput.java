package auction.input;

import java.util.UUID;
import lombok.Builder;

@Builder
public record GetAuctionInput(UUID auctionId) {}
