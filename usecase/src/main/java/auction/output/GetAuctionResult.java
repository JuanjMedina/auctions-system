package auction.output;

import domain.auction.AuctionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GetAuctionResult(
    UUID id,
    UUID sellerId,
    UUID categoryId,
    String title,
    String description,
    BigDecimal startingPrice,
    BigDecimal reservePrice,
    BigDecimal currentPrice,
    UUID currentWinnerId,
    AuctionStatus status,
    Instant startsAt,
    Instant endsAt,
    Instant closedAt,
    boolean autoExtend,
    int extendMinutes,
    List<AuctionImageResult> images,
    Instant createdAt,
    Instant updatedAt) {

  public record AuctionImageResult(UUID id, String url, boolean primary, int displayOrder) {}
}
