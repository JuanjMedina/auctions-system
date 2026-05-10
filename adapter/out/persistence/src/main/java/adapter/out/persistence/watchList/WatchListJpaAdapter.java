package adapter.out.persistence.watchList;

import domain.watchList.WatchList;
import domain.watchList.WatchListRepository;
import entity.auction.AuctionJpaEntity;
import entity.user.UserEntity;
import entity.watchList.WatchListJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import repository.watchList.SpringDataWatchListRepository;

@Component
@RequiredArgsConstructor
public class WatchListJpaAdapter implements WatchListRepository {

  private final SpringDataWatchListRepository springDataRepo;

  @Override
  public WatchList save(WatchList watchList) {
    return toDomain(springDataRepo.save(toJpaEntity(watchList)));
  }

  @Override
  public Optional<WatchList> findByUserIdAndAuctionId(UUID userId, UUID auctionId) {
    return springDataRepo.findByUser_IdAndAuction_Id(userId, auctionId).map(this::toDomain);
  }

  @Override
  public List<WatchList> findByUserId(UUID userId) {
    return springDataRepo.findByUser_IdOrderByAddedAtDesc(userId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<WatchList> findByAuctionId(UUID auctionId) {
    return springDataRepo.findByAuction_Id(auctionId).stream().map(this::toDomain).toList();
  }

  @Override
  public boolean existsByUserIdAndAuctionId(UUID userId, UUID auctionId) {
    return springDataRepo.existsByUser_IdAndAuction_Id(userId, auctionId);
  }

  @Override
  public void delete(WatchList watchList) {
    springDataRepo.deleteById(watchList.getId());
  }

  private WatchListJpaEntity toJpaEntity(WatchList watchList) {
    return WatchListJpaEntity.builder()
        .id(watchList.getId())
        .user(UserEntity.builder().id(watchList.getUserId()).build())
        .auction(AuctionJpaEntity.builder().id(watchList.getAuctionId()).build())
        .build();
  }

  private WatchList toDomain(WatchListJpaEntity entity) {
    return WatchList.reconstitute(
        entity.getId(), entity.getUserId(), entity.getAuctionId(), entity.getAddedAt());
  }
}
