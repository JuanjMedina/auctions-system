package watchList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import domain.auction.Auction;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import domain.watchList.WatchList;
import domain.watchList.WatchListRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import watchList.input.ListWatchListInput;
import watchList.output.ListWatchListResult;

@ExtendWith(MockitoExtension.class)
class ListWatchListUseCaseTest {

  @Mock private WatchListRepository watchListRepository;
  @Mock private AuctionRepository auctionRepository;

  @InjectMocks private ListWatchListUseCase useCase;

  // --- fixtures ---
  private static final UUID USER_ID = UUID.randomUUID();

  private Auction buildAuction(UUID auctionId) {
    return Auction.reconstitute(
        auctionId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Subasta test",
        "Descripcion",
        BigDecimal.TEN,
        BigDecimal.TEN,
        BigDecimal.TEN,
        null,
        AuctionStatus.ACTIVE,
        Instant.now(),
        Instant.now().plusSeconds(3600),
        null,
        false,
        0,
        List.of(),
        Instant.now(),
        Instant.now(),
        0L);
  }

  private WatchList buildWatchList(UUID auctionId) {
    return WatchList.reconstitute(UUID.randomUUID(), USER_ID, auctionId, Instant.now());
  }

  // --- happy path ---

  @Test
  void execute_userWithWatchedAuctions_returnsEntriesWithAuctionSummaries() {
    // arrange
    UUID auctionId1 = UUID.randomUUID();
    UUID auctionId2 = UUID.randomUUID();
    WatchList watch1 = buildWatchList(auctionId1);
    WatchList watch2 = buildWatchList(auctionId2);

    when(watchListRepository.findByUserId(USER_ID)).thenReturn(List.of(watch1, watch2));
    when(auctionRepository.findById(auctionId1)).thenReturn(Optional.of(buildAuction(auctionId1)));
    when(auctionRepository.findById(auctionId2)).thenReturn(Optional.of(buildAuction(auctionId2)));

    // act
    ListWatchListResult result = useCase.run(new ListWatchListInput(USER_ID));

    // assert
    assertThat(result.entries()).hasSize(2);
    assertThat(result.entries())
        .extracting(entry -> entry.auction().id())
        .containsExactlyInAnyOrder(auctionId1, auctionId2);
  }

  @Test
  void execute_userWithWatchedAuctions_entryContainsWatchListIdAndAddedAt() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    WatchList watch = buildWatchList(auctionId);

    when(watchListRepository.findByUserId(USER_ID)).thenReturn(List.of(watch));
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(buildAuction(auctionId)));

    // act
    ListWatchListResult result = useCase.run(new ListWatchListInput(USER_ID));

    // assert
    assertThat(result.entries()).hasSize(1);
    assertThat(result.entries().get(0).watchListId()).isEqualTo(watch.getId());
    assertThat(result.entries().get(0).addedAt()).isEqualTo(watch.getAddedAt());
  }

  // --- lista vacía ---

  @Test
  void execute_userWithNoWatchedAuctions_returnsEmptyList() {
    // arrange
    when(watchListRepository.findByUserId(USER_ID)).thenReturn(List.of());

    // act
    ListWatchListResult result = useCase.run(new ListWatchListInput(USER_ID));

    // assert
    assertThat(result.entries()).isEmpty();
  }

  // --- subasta referenciada ya no existe (huérfana) ---

  @Test
  void execute_watchedAuctionNoLongerExists_filtersOutOrphanEntry() {
    // arrange
    UUID auctionId1 = UUID.randomUUID();
    UUID auctionId2 = UUID.randomUUID();
    WatchList watch1 = buildWatchList(auctionId1);
    WatchList watch2 = buildWatchList(auctionId2);

    when(watchListRepository.findByUserId(USER_ID)).thenReturn(List.of(watch1, watch2));
    when(auctionRepository.findById(auctionId1)).thenReturn(Optional.of(buildAuction(auctionId1)));
    when(auctionRepository.findById(auctionId2)).thenReturn(Optional.empty());

    // act
    ListWatchListResult result = useCase.run(new ListWatchListInput(USER_ID));

    // assert
    assertThat(result.entries()).hasSize(1);
    assertThat(result.entries().get(0).auction().id()).isEqualTo(auctionId1);
  }
}
