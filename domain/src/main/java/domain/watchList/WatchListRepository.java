package domain.watchList;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchListRepository {
  WatchList save(WatchList watchList);

  Optional<WatchList> findByUserIdAndAuctionId(UUID userId, UUID auctionId);

  List<WatchList> findByUserId(UUID userId);

  List<WatchList> findByAuctionId(UUID auctionId);

  boolean existsByUserIdAndAuctionId(UUID userId, UUID auctionId);

  void delete(WatchList watchList);
}
