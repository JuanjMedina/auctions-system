package domain.bid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepository {
  Bid save(Bid bid);

  Optional<Bid> findById(UUID id);

  List<Bid> findByAuctionIdOrderByAmountDesc(UUID auctionId);

  List<Bid> findByBidderId(UUID bidderId);

  List<Bid> findActiveByAuctionId(UUID auctionId);

  void saveAll(List<Bid> bids);
}
