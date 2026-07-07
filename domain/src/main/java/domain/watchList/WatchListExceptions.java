package domain.watchList;

import java.util.UUID;

public final class WatchListExceptions {

  private WatchListExceptions() {}

  public static class AlreadyInWatchListException extends RuntimeException {
    public AlreadyInWatchListException(UUID userId, UUID auctionId) {
      super(
          String.format(
              "La subasta %s ya esta en la lista de favoritos del usuario %s", auctionId, userId));
    }
  }

  public static class WatchListEntryNotFoundException extends RuntimeException {
    public WatchListEntryNotFoundException(UUID userId, UUID auctionId) {
      super(
          String.format(
              "La subasta %s no esta en la lista de favoritos del usuario %s", auctionId, userId));
    }
  }
}
