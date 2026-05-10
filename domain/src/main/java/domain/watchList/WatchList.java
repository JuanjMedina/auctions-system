package domain.watchList;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = lombok.AccessLevel.PRIVATE)
public class WatchList {

  private final UUID id;
  private final UUID userId;
  private final UUID auctionId;
  private final Instant addedAt;

  public static WatchList create(UUID userId, UUID auctionId) {
    return WatchList.builder()
        .id(UUID.randomUUID())
        .userId(userId)
        .auctionId(auctionId)
        .addedAt(Instant.now())
        .build();
  }

  public static WatchList reconstitute(UUID id, UUID userId, UUID auctionId, Instant addedAt) {
    return WatchList.builder().id(id).userId(userId).auctionId(auctionId).addedAt(addedAt).build();
  }
}
