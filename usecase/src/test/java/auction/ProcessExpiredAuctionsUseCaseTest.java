package auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import auction.input.CloseAuctionInput;
import auction.output.CloseAuctionResult;
import auction.output.ProcessAuctionsResult;
import domain.auction.Auction;
import domain.auction.AuctionExceptions.InvalidAuctionStatusTransitionException;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shared.NoInput;

@ExtendWith(MockitoExtension.class)
class ProcessExpiredAuctionsUseCaseTest {

  @Mock private AuctionRepository auctionRepository;
  @Mock private CloseAuctionUseCase closeAuctionUseCase;

  @InjectMocks private ProcessExpiredAuctionsUseCase useCase;

  // --- fixtures ---

  private Auction buildAuction(UUID id, AuctionStatus status) {
    return Auction.reconstitute(
        id,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Subasta test",
        "Descripcion",
        BigDecimal.TEN,
        BigDecimal.valueOf(20),
        BigDecimal.TEN,
        null,
        status,
        Instant.now().minusSeconds(3600),
        Instant.now().minusSeconds(1),
        null,
        false,
        5,
        List.of(),
        Instant.now(),
        Instant.now(),
        0L);
  }

  // --- sin subastas expiradas ---

  @Test
  void execute_noExpiredAuctions_returnsZeroProcessedAndFailed() {
    // arrange
    when(auctionRepository.findExpiredActiveAuctions()).thenReturn(List.of());

    // act
    ProcessAuctionsResult result = useCase.run(NoInput.INSTANCE);

    // assert
    assertThat(result.processed()).isZero();
    assertThat(result.failed()).isZero();
    verifyNoInteractions(closeAuctionUseCase);
  }

  // --- happy path ---

  @Test
  void execute_expiredAuctions_closesEachAndCountsProcessed() {
    // arrange
    UUID auctionId1 = UUID.randomUUID();
    UUID auctionId2 = UUID.randomUUID();
    Auction auction1 = buildAuction(auctionId1, AuctionStatus.ACTIVE);
    Auction auction2 = buildAuction(auctionId2, AuctionStatus.ACTIVE);

    when(auctionRepository.findExpiredActiveAuctions()).thenReturn(List.of(auction1, auction2));
    when(closeAuctionUseCase.run(new CloseAuctionInput(auctionId1)))
        .thenReturn(new CloseAuctionResult(auctionId1, AuctionStatus.FAILED, null));
    when(closeAuctionUseCase.run(new CloseAuctionInput(auctionId2)))
        .thenReturn(new CloseAuctionResult(auctionId2, AuctionStatus.AWARDED, UUID.randomUUID()));

    // act
    ProcessAuctionsResult result = useCase.run(NoInput.INSTANCE);

    // assert
    assertThat(result.processed()).isEqualTo(2);
    assertThat(result.failed()).isZero();
    verify(closeAuctionUseCase, times(2)).run(any());
  }

  // --- una falla, la otra se procesa ---

  @Test
  void execute_oneAuctionFailsToClose_continuesAndCountsFailure() {
    // arrange
    UUID auctionId1 = UUID.randomUUID();
    UUID auctionId2 = UUID.randomUUID();
    Auction auction1 = buildAuction(auctionId1, AuctionStatus.ACTIVE);
    Auction auction2 = buildAuction(auctionId2, AuctionStatus.ACTIVE);

    when(auctionRepository.findExpiredActiveAuctions()).thenReturn(List.of(auction1, auction2));
    when(closeAuctionUseCase.run(new CloseAuctionInput(auctionId1)))
        .thenThrow(
            new InvalidAuctionStatusTransitionException(
                auctionId1, AuctionStatus.ACTIVE, AuctionStatus.CLOSED));
    when(closeAuctionUseCase.run(new CloseAuctionInput(auctionId2)))
        .thenReturn(new CloseAuctionResult(auctionId2, AuctionStatus.FAILED, null));

    // act
    ProcessAuctionsResult result = useCase.run(NoInput.INSTANCE);

    // assert
    assertThat(result.processed()).isEqualTo(1);
    assertThat(result.failed()).isEqualTo(1);
  }

  @Test
  void execute_allAuctionsFail_returnsAllFailedNoneProcessed() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, AuctionStatus.ACTIVE);

    when(auctionRepository.findExpiredActiveAuctions()).thenReturn(List.of(auction));
    when(closeAuctionUseCase.run(new CloseAuctionInput(auctionId)))
        .thenThrow(new RuntimeException("boom"));

    // act
    ProcessAuctionsResult result = useCase.run(NoInput.INSTANCE);

    // assert
    assertThat(result.processed()).isZero();
    assertThat(result.failed()).isEqualTo(1);
  }
}
