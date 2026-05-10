package domain.bid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = lombok.AccessLevel.PRIVATE)
public class Bid {

  private final UUID id;
  private final UUID auctionId;
  private final UUID bidderId;
  private final BigDecimal amount;
  private final boolean autoBid;
  private final BigDecimal maxAutoAmount;
  private BidStatus status;
  private final Instant createdAt;

  public static Bid create(
      UUID auctionId, UUID bidderId, BigDecimal amount, boolean autoBid, BigDecimal maxAutoAmount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
      throw new IllegalArgumentException("El monto de la puja debe ser mayor a cero");
    if (autoBid && (maxAutoAmount == null || maxAutoAmount.compareTo(amount) < 0))
      throw new IllegalArgumentException(
          "El monto maximo de auto-puja debe ser >= al monto de la puja");
    if (!autoBid && maxAutoAmount != null)
      throw new IllegalArgumentException("Solo una auto-puja puede tener monto maximo");

    return Bid.builder()
        .id(UUID.randomUUID())
        .auctionId(auctionId)
        .bidderId(bidderId)
        .amount(amount)
        .autoBid(autoBid)
        .maxAutoAmount(maxAutoAmount)
        .status(BidStatus.ACTIVE)
        .createdAt(Instant.now())
        .build();
  }

  public static Bid reconstitute(
      UUID id,
      UUID auctionId,
      UUID bidderId,
      BigDecimal amount,
      boolean autoBid,
      BigDecimal maxAutoAmount,
      BidStatus status,
      Instant createdAt) {
    return Bid.builder()
        .id(id)
        .auctionId(auctionId)
        .bidderId(bidderId)
        .amount(amount)
        .autoBid(autoBid)
        .maxAutoAmount(maxAutoAmount)
        .status(status)
        .createdAt(createdAt)
        .build();
  }

  public void markAsOutbid() {
    if (status != BidStatus.ACTIVE)
      throw new BidExceptions.InvalidBidStatusTransitionException(id, status, BidStatus.OUTBID);
    this.status = BidStatus.OUTBID;
  }

  public void markAsWinning() {
    if (status != BidStatus.ACTIVE)
      throw new BidExceptions.InvalidBidStatusTransitionException(id, status, BidStatus.WINNING);
    this.status = BidStatus.WINNING;
  }

  public void cancel() {
    if (status == BidStatus.WINNING || status == BidStatus.OUTBID)
      throw new BidExceptions.InvalidBidStatusTransitionException(id, status, BidStatus.CANCELLED);
    this.status = BidStatus.CANCELLED;
  }
}
