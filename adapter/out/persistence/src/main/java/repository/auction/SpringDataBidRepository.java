package repository.auction;

import domain.bid.BidStatus;
import entity.auction.BidJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataBidRepository extends JpaRepository<BidJpaEntity, UUID> {

  List<BidJpaEntity> findByAuctionIdOrderByAmountDesc(UUID auctionId);

  List<BidJpaEntity> findByBidderIdOrderByCreatedAtDesc(UUID bidderId);

  @Query(
      """
        SELECT b FROM BidJpaEntity b
        WHERE b.auctionId = :auctionId
        AND b.status = :status
        ORDER BY b.amount DESC
        """)
  List<BidJpaEntity> findByAuctionIdAndStatus(
      @Param("auctionId") UUID auctionId, @Param("status") BidStatus status);

  default List<BidJpaEntity> findActiveByAuctionId(UUID auctionId) {
    return findByAuctionIdAndStatus(auctionId, BidStatus.ACTIVE);
  }

  @Query(
      """
        SELECT b FROM BidJpaEntity b
        WHERE b.auctionId = :auctionId
        AND b.bidderId = :bidderId
        AND b.status = 'ACTIVE'
        ORDER BY b.amount DESC
        LIMIT 1
        """)
  Optional<BidJpaEntity> findLatestActiveBidByAuctionIdAndBidderId(
      @Param("auctionId") UUID auctionId, @Param("bidderId") UUID bidderId);
}
