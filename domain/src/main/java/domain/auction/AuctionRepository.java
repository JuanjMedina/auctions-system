package domain.auction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionRepository {
    Auction save(Auction auction);
    Optional<Auction> findById(UUID id);
    List<Auction> findExpiredActiveAuctions();   // usada por el scheduler cada 30s
    List<Auction> findScheduledReadyToStart();   // usada por el scheduler
    List<Auction> findBySellerId(UUID sellerId);
    List<Auction> findActiveByCategoryId(UUID categoryId);
    boolean existsByIdAndSellerId(UUID id, UUID sellerId);
}
