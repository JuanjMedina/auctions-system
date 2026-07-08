package adapter.watchList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.watchList.WatchList;
import entity.watchList.WatchListJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.watchList.SpringDataWatchListRepository;

@ExtendWith(MockitoExtension.class)
class WatchListJpaAdapterTest {

  @Mock private SpringDataWatchListRepository springDataRepo;

  private WatchListJpaAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new WatchListJpaAdapter(springDataRepo);
  }

  private WatchList buildWatchList(UUID id, UUID userId, UUID auctionId) {
    return WatchList.reconstitute(id, userId, auctionId, Instant.now());
  }

  private WatchListJpaEntity buildEntity(UUID id, UUID userId, UUID auctionId) {
    return WatchListJpaEntity.builder()
        .id(id)
        .userId(userId)
        .auctionId(auctionId)
        .addedAt(Instant.now())
        .build();
  }

  @Test
  void save_delegatesToRepositoryAndMapsResult() {
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID auctionId = UUID.randomUUID();
    WatchList watchList = buildWatchList(id, userId, auctionId);
    WatchListJpaEntity savedEntity = buildEntity(id, userId, auctionId);

    when(springDataRepo.save(any(WatchListJpaEntity.class))).thenReturn(savedEntity);

    WatchList result = adapter.save(watchList);

    ArgumentCaptor<WatchListJpaEntity> captor = ArgumentCaptor.forClass(WatchListJpaEntity.class);
    verify(springDataRepo).save(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(id);
    assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    assertThat(captor.getValue().getAuctionId()).isEqualTo(auctionId);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getAuctionId()).isEqualTo(auctionId);
  }

  @Test
  void findByUserIdAndAuctionId_existingEntry_returnsMappedDomain() {
    UUID userId = UUID.randomUUID();
    UUID auctionId = UUID.randomUUID();
    WatchListJpaEntity entity = buildEntity(UUID.randomUUID(), userId, auctionId);
    when(springDataRepo.findByUserIdAndAuctionId(userId, auctionId))
        .thenReturn(Optional.of(entity));

    Optional<WatchList> result = adapter.findByUserIdAndAuctionId(userId, auctionId);

    assertThat(result).isPresent();
    assertThat(result.get().getUserId()).isEqualTo(userId);
    assertThat(result.get().getAuctionId()).isEqualTo(auctionId);
  }

  @Test
  void findByUserIdAndAuctionId_missingEntry_returnsEmptyOptional() {
    UUID userId = UUID.randomUUID();
    UUID auctionId = UUID.randomUUID();
    when(springDataRepo.findByUserIdAndAuctionId(userId, auctionId)).thenReturn(Optional.empty());

    Optional<WatchList> result = adapter.findByUserIdAndAuctionId(userId, auctionId);

    assertThat(result).isEmpty();
  }

  @Test
  void findByUserId_delegatesToOrderByAddedAtDescAndMapsResults() {
    UUID userId = UUID.randomUUID();
    WatchListJpaEntity entity = buildEntity(UUID.randomUUID(), userId, UUID.randomUUID());
    when(springDataRepo.findByUserIdOrderByAddedAtDesc(userId)).thenReturn(List.of(entity));

    List<WatchList> result = adapter.findByUserId(userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getUserId()).isEqualTo(userId);
  }

  @Test
  void findByUserId_noEntries_returnsEmptyList() {
    UUID userId = UUID.randomUUID();
    when(springDataRepo.findByUserIdOrderByAddedAtDesc(userId)).thenReturn(List.of());

    List<WatchList> result = adapter.findByUserId(userId);

    assertThat(result).isEmpty();
  }

  @Test
  void findByAuctionId_delegatesAndMapsResults() {
    UUID auctionId = UUID.randomUUID();
    WatchListJpaEntity entity = buildEntity(UUID.randomUUID(), UUID.randomUUID(), auctionId);
    when(springDataRepo.findByAuctionId(auctionId)).thenReturn(List.of(entity));

    List<WatchList> result = adapter.findByAuctionId(auctionId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAuctionId()).isEqualTo(auctionId);
  }

  @Test
  void existsByUserIdAndAuctionId_entryExists_returnsTrue() {
    UUID userId = UUID.randomUUID();
    UUID auctionId = UUID.randomUUID();
    when(springDataRepo.existsByUserIdAndAuctionId(userId, auctionId)).thenReturn(true);

    boolean result = adapter.existsByUserIdAndAuctionId(userId, auctionId);

    assertThat(result).isTrue();
  }

  @Test
  void existsByUserIdAndAuctionId_entryMissing_returnsFalse() {
    UUID userId = UUID.randomUUID();
    UUID auctionId = UUID.randomUUID();
    when(springDataRepo.existsByUserIdAndAuctionId(userId, auctionId)).thenReturn(false);

    boolean result = adapter.existsByUserIdAndAuctionId(userId, auctionId);

    assertThat(result).isFalse();
  }

  @Test
  void delete_delegatesDeleteByIdWithWatchListId() {
    UUID id = UUID.randomUUID();
    WatchList watchList = buildWatchList(id, UUID.randomUUID(), UUID.randomUUID());

    adapter.delete(watchList);

    verify(springDataRepo).deleteById(id);
  }
}
