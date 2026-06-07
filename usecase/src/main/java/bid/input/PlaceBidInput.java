package bid.input;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceBidInput(
    UUID auctionId, UUID bidderId, BigDecimal amount, boolean autoBid, BigDecimal maxAutoAmount) {}
