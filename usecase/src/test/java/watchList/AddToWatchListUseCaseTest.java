package watchList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.auction.Auction;
import domain.auction.AuctionExceptions.AuctionNotFoundException;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import domain.watchList.WatchListExceptions.AlreadyInWatchListException;
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
import watchList.input.AddToWatchListInput;
import watchList.output.AddToWatchListResult;

@ExtendWith(MockitoExtension.class)
class AddToWatchListUseCaseTest {

  @Mock private AuctionRepository auctionRepository;
  @Mock private WatchListRepository watchListRepository;

  @InjectMocks private AddToWatchListUseCase useCase;

  // --- fixtures ---
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID AUCTION_ID = UUID.randomUUID();

  private Auction buildAuction() {
    return Auction.reconstitute(
        AUCTION_ID,
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

  private AddToWatchListInput validInput() {
    return new AddToWatchListInput(USER_ID, AUCTION_ID);
  }

  // --- happy path ---

  @Test
  void execute_validAuction_returnsResultWithAuctionId() {
    // arrange
    when(auctionRepository.findById(AUCTION_ID)).thenReturn(Optional.of(buildAuction()));
    when(watchListRepository.existsByUserIdAndAuctionId(USER_ID, AUCTION_ID)).thenReturn(false);
    when(watchListRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    AddToWatchListResult result = useCase.run(validInput());

    // assert
    assertThat(result.auctionId()).isEqualTo(AUCTION_ID);
    assertThat(result.id()).isNotNull();
    assertThat(result.addedAt()).isNotNull();
  }

  @Test
  void execute_validAuction_persistsWatchListEntry() {
    // arrange
    when(auctionRepository.findById(AUCTION_ID)).thenReturn(Optional.of(buildAuction()));
    when(watchListRepository.existsByUserIdAndAuctionId(USER_ID, AUCTION_ID)).thenReturn(false);
    when(watchListRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    useCase.run(validInput());

    // assert
    verify(watchListRepository).save(any());
  }

  // --- subasta no encontrada ---

  @Test
  void execute_auctionNotFound_throwsAuctionNotFoundException() {
    // arrange
    when(auctionRepository.findById(AUCTION_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(AuctionNotFoundException.class);
  }

  @Test
  void execute_auctionNotFound_neverChecksWatchListOrSaves() {
    // arrange
    when(auctionRepository.findById(AUCTION_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(AuctionNotFoundException.class);

    verify(watchListRepository, never()).existsByUserIdAndAuctionId(any(), any());
    verify(watchListRepository, never()).save(any());
  }

  // --- ya está en la watchlist ---

  @Test
  void execute_alreadyInWatchList_throwsAlreadyInWatchListException() {
    // arrange
    when(auctionRepository.findById(AUCTION_ID)).thenReturn(Optional.of(buildAuction()));
    when(watchListRepository.existsByUserIdAndAuctionId(USER_ID, AUCTION_ID)).thenReturn(true);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(AlreadyInWatchListException.class);
  }

  @Test
  void execute_alreadyInWatchList_neverSaves() {
    // arrange
    when(auctionRepository.findById(AUCTION_ID)).thenReturn(Optional.of(buildAuction()));
    when(watchListRepository.existsByUserIdAndAuctionId(USER_ID, AUCTION_ID)).thenReturn(true);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(AlreadyInWatchListException.class);

    verify(watchListRepository, never()).save(any());
  }
}
