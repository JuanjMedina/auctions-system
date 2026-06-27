package auction.output;

import domain.auction.AuctionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AuctionSummary(
    UUID id,
    String title,
    String description,
    UUID categoryId,
    UUID sellerId,
    BigDecimal currentPrice,
    AuctionStatus status,
    Instant startsAt,
    Instant endsAt) {}
