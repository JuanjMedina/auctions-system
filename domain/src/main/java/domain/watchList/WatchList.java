package domain.watchList;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
public class WatchList {

  private final UUID id;
  private final UUID userId;
  private final UUID auctionId;
  private final Instant addedAt;

  private WatchList(UUID id, UUID userId, UUID auctionId, Instant addedAt) {
    this.id = id;
    this.userId = userId;
    this.auctionId = auctionId;
    this.addedAt = addedAt;
  }

  public static WatchList create(UUID userId, UUID auctionId) {
    return new WatchList(UUID.randomUUID(), userId, auctionId, Instant.now());
  }

  public static WatchList reconstitute(UUID id, UUID userId, UUID auctionId, Instant addedAt) {
    return new WatchList(id, userId, auctionId, addedAt);
  }
}
