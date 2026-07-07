package repository.watchList;

import entity.watchList.WatchListJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataWatchListRepository extends JpaRepository<WatchListJpaEntity, UUID> {

  Optional<WatchListJpaEntity> findByUserIdAndAuctionId(UUID userId, UUID auctionId);

  List<WatchListJpaEntity> findByUserIdOrderByAddedAtDesc(UUID userId);

  List<WatchListJpaEntity> findByAuctionId(UUID auctionId);

  boolean existsByUserIdAndAuctionId(UUID userId, UUID auctionId);
}
