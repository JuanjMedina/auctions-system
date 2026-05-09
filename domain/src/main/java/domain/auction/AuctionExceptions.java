package domain.auction;

import java.math.BigDecimal;
import java.util.UUID;

public final class AuctionExceptions {

  private AuctionExceptions() {}

  public static class AuctionNotFoundException extends RuntimeException {
    private final UUID auctionId;

    public AuctionNotFoundException(UUID auctionId) {
      super("Subasta no encontrada: " + auctionId);
      this.auctionId = auctionId;
    }

    public UUID getAuctionId() {
      return auctionId;
    }
  }

  public static class AuctionNotActiveException extends RuntimeException {
    public AuctionNotActiveException(UUID auctionId, AuctionStatus currentStatus) {
      super(String.format("La subasta %s no acepta pujas en estado %s", auctionId, currentStatus));
    }
  }

  public static class InvalidAuctionStatusTransitionException extends RuntimeException {
    public InvalidAuctionStatusTransitionException(
        UUID auctionId, AuctionStatus from, AuctionStatus to) {
      super(String.format("Transicion invalida en subasta %s: %s -> %s", auctionId, from, to));
    }
  }

  public static class BidTooLowException extends RuntimeException {
    private final BigDecimal attemptedAmount;
    private final BigDecimal currentPrice;

    public BidTooLowException(UUID auctionId, BigDecimal attemptedAmount, BigDecimal currentPrice) {
      super(
          String.format("Puja de %s rechazada — precio actual: %s", attemptedAmount, currentPrice));
      this.attemptedAmount = attemptedAmount;
      this.currentPrice = currentPrice;
    }

    public BigDecimal getAttemptedAmount() {
      return attemptedAmount;
    }

    public BigDecimal getCurrentPrice() {
      return currentPrice;
    }
  }

  public static class SellerCannotBidException extends RuntimeException {
    public SellerCannotBidException(UUID auctionId) {
      super("El vendedor no puede pujar en su propia subasta: " + auctionId);
    }
  }
}
