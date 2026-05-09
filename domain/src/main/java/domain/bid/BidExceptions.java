package domain.bid;

import java.util.UUID;

public final class BidExceptions {

  private BidExceptions() {}

  public static class BidNotFoundException extends RuntimeException {
    public BidNotFoundException(UUID bidId) {
      super("Puja no encontrada: " + bidId);
    }
  }

  public static class InvalidBidStatusTransitionException extends RuntimeException {
    public InvalidBidStatusTransitionException(UUID bidId, BidStatus from, BidStatus to) {
      super(String.format("Transicion invalida en puja %s: %s -> %s", bidId, from, to));
    }
  }
}
