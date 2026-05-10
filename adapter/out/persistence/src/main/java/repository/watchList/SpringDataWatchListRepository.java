package repository.watchList;

import entity.watchList.WatchListJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataWatchListRepository extends JpaRepository<WatchListJpaEntity, UUID> {

  Optional<WatchListJpaEntity> findByUser_IdAndAuction_Id(UUID userId, UUID auctionId);

  List<WatchListJpaEntity> findByUser_IdOrderByAddedAtDesc(UUID userId);

  List<WatchListJpaEntity> findByAuction_Id(UUID auctionId);

  boolean existsByUser_IdAndAuction_Id(UUID userId, UUID auctionId);
}
