package repository.auction;

import domain.auction.AuctionStatus;
import entity.auction.AuctionJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAuctionRepository
    extends JpaRepository<AuctionJpaEntity, UUID>, JpaSpecificationExecutor<AuctionJpaEntity> {

  // Subastas ACTIVE o EXTENDED cuyo ends_at ya paso
  @Query(
      """
        SELECT a FROM AuctionJpaEntity a
        WHERE a.status IN :statuses
        AND a.endsAt < CURRENT_TIMESTAMP
        ORDER BY a.endsAt ASC
        """)
  List<AuctionJpaEntity> findExpiredByStatuses(@Param("statuses") List<AuctionStatus> statuses);

  default List<AuctionJpaEntity> findExpiredActiveAuctions() {
    return findExpiredByStatuses(List.of(AuctionStatus.ACTIVE, AuctionStatus.EXTENDED));
  }

  // Subastas SCHEDULED cuyo starts_at ya llego
  @Query(
      """
        SELECT a FROM AuctionJpaEntity a
        WHERE a.status = :status
        AND a.startsAt <= CURRENT_TIMESTAMP
        """)
  List<AuctionJpaEntity> findByStatusAndStartsAtReady(@Param("status") AuctionStatus status);

  default List<AuctionJpaEntity> findScheduledReadyToStart() {
    return findByStatusAndStartsAtReady(AuctionStatus.SCHEDULED);
  }

  List<AuctionJpaEntity> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

  @Query(
      """
        SELECT a FROM AuctionJpaEntity a
        WHERE a.categoryId = :categoryId
        AND a.status IN :statuses
        ORDER BY a.endsAt ASC
        """)
  List<AuctionJpaEntity> findByCategoryIdAndStatuses(
      @Param("categoryId") UUID categoryId, @Param("statuses") List<AuctionStatus> statuses);

  default List<AuctionJpaEntity> findActiveByCategoryId(UUID categoryId) {
    return findByCategoryIdAndStatuses(
        categoryId, List.of(AuctionStatus.ACTIVE, AuctionStatus.EXTENDED));
  }

  boolean existsByIdAndSellerId(UUID id, UUID sellerId);
}
