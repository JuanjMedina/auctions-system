package auction.input;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateAuctionInput(
    UUID sellerId,
    UUID categoryId,
    String title,
    String description,
    BigDecimal startingPrice,
    BigDecimal reservePrice,
    Instant startsAt,
    Instant endsAt,
    boolean autoExtend,
    int extendMinutes) {}
