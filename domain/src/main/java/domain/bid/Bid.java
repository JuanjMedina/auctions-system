package domain.bid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Bid {

  private final UUID id;
  private final UUID auctionId;
  private final UUID bidderId;
  private final BigDecimal amount;
  private final boolean autoBid;
  private final BigDecimal maxAutoAmount; // null si no es auto-bid
  private BidStatus status; // mutable: ACTIVE -> OUTBID / WINNING
  private final Instant createdAt;

  private Bid(
      UUID id,
      UUID auctionId,
      UUID bidderId,
      BigDecimal amount,
      boolean autoBid,
      BigDecimal maxAutoAmount,
      BidStatus status,
      Instant createdAt) {
    this.id = id;
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.amount = amount;
    this.autoBid = autoBid;
    this.maxAutoAmount = maxAutoAmount;
    this.status = status;
    this.createdAt = createdAt;
  }

  // Factory method: puja nueva
  public static Bid create(
      UUID auctionId, UUID bidderId, BigDecimal amount, boolean autoBid, BigDecimal maxAutoAmount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
      throw new IllegalArgumentException("El monto de la puja debe ser mayor a cero");
    if (autoBid && (maxAutoAmount == null || maxAutoAmount.compareTo(amount) < 0))
      throw new IllegalArgumentException(
          "El monto maximo de auto-puja debe ser >= al monto de la puja");
    if (!autoBid && maxAutoAmount != null)
      throw new IllegalArgumentException("Solo una auto-puja puede tener monto maximo");
    return new Bid(
        UUID.randomUUID(),
        auctionId,
        bidderId,
        amount,
        autoBid,
        maxAutoAmount,
        BidStatus.ACTIVE,
        Instant.now());
  }

  // Factory method: reconstruir desde persistencia
  public static Bid reconstitute(
      UUID id,
      UUID auctionId,
      UUID bidderId,
      BigDecimal amount,
      boolean autoBid,
      BigDecimal maxAutoAmount,
      BidStatus status,
      Instant createdAt) {
    return new Bid(id, auctionId, bidderId, amount, autoBid, maxAutoAmount, status, createdAt);
  }

  // Logica de negocio: marcar como superada
  public void markAsOutbid() {
    if (status != BidStatus.ACTIVE)
      throw new BidExceptions.InvalidBidStatusTransitionException(id, status, BidStatus.OUTBID);
    this.status = BidStatus.OUTBID;
  }

  // Logica de negocio: marcar como ganadora al cierre
  public void markAsWinning() {
    if (status != BidStatus.ACTIVE)
      throw new BidExceptions.InvalidBidStatusTransitionException(id, status, BidStatus.WINNING);
    this.status = BidStatus.WINNING;
  }

  // Logica de negocio: cancelar puja
  public void cancel() {
    if (status == BidStatus.WINNING || status == BidStatus.OUTBID)
      throw new BidExceptions.InvalidBidStatusTransitionException(id, status, BidStatus.CANCELLED);
    this.status = BidStatus.CANCELLED;
  }
}
