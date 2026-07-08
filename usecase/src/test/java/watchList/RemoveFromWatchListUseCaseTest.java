package watchList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.watchList.WatchList;
import domain.watchList.WatchListExceptions.WatchListEntryNotFoundException;
import domain.watchList.WatchListRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import watchList.input.RemoveFromWatchListInput;
import watchList.output.RemoveFromWatchListResult;

@ExtendWith(MockitoExtension.class)
class RemoveFromWatchListUseCaseTest {

  @Mock private WatchListRepository watchListRepository;

  @InjectMocks private RemoveFromWatchListUseCase useCase;

  // --- fixtures ---
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID AUCTION_ID = UUID.randomUUID();

  private WatchList buildWatchList() {
    return WatchList.reconstitute(UUID.randomUUID(), USER_ID, AUCTION_ID, Instant.now());
  }

  private RemoveFromWatchListInput validInput() {
    return new RemoveFromWatchListInput(USER_ID, AUCTION_ID);
  }

  // --- happy path ---

  @Test
  void execute_existingEntry_returnsResultWithAuctionId() {
    // arrange
    when(watchListRepository.findByUserIdAndAuctionId(USER_ID, AUCTION_ID))
        .thenReturn(Optional.of(buildWatchList()));

    // act
    RemoveFromWatchListResult result = useCase.run(validInput());

    // assert
    assertThat(result.auctionId()).isEqualTo(AUCTION_ID);
  }

  @Test
  void execute_existingEntry_deletesEntryFromRepository() {
    // arrange
    WatchList entry = buildWatchList();
    when(watchListRepository.findByUserIdAndAuctionId(USER_ID, AUCTION_ID))
        .thenReturn(Optional.of(entry));

    // act
    useCase.run(validInput());

    // assert
    verify(watchListRepository).delete(entry);
  }

  // --- entrada no encontrada ---

  @Test
  void execute_entryNotFound_throwsWatchListEntryNotFoundException() {
    // arrange
    when(watchListRepository.findByUserIdAndAuctionId(USER_ID, AUCTION_ID))
        .thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WatchListEntryNotFoundException.class);
  }

  @Test
  void execute_entryNotFound_neverCallsDelete() {
    // arrange
    when(watchListRepository.findByUserIdAndAuctionId(USER_ID, AUCTION_ID))
        .thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WatchListEntryNotFoundException.class);

    verify(watchListRepository, never()).delete(org.mockito.ArgumentMatchers.any());
  }
}
