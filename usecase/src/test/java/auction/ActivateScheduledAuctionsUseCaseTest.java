package auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import auction.input.ActivateAuctionInput;
import auction.output.ActivateAuctionResult;
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
class ActivateScheduledAuctionsUseCaseTest {

  @Mock private AuctionRepository auctionRepository;
  @Mock private ActivateAuctionUseCase activateAuctionUseCase;

  @InjectMocks private ActivateScheduledAuctionsUseCase useCase;

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
        Instant.now().minusSeconds(60),
        Instant.now().plusSeconds(3600),
        null,
        false,
        5,
        List.of(),
        Instant.now(),
        Instant.now(),
        0L);
  }

  // --- sin subastas para activar ---

  @Test
  void execute_noScheduledAuctions_returnsZeroProcessedAndFailed() {
    // arrange
    when(auctionRepository.findScheduledReadyToStart()).thenReturn(List.of());

    // act
    ProcessAuctionsResult result = useCase.run(NoInput.INSTANCE);

    // assert
    assertThat(result.processed()).isZero();
    assertThat(result.failed()).isZero();
    verifyNoInteractions(activateAuctionUseCase);
  }

  // --- happy path ---

  @Test
  void execute_scheduledAuctionsReady_activatesEachAndCountsProcessed() {
    // arrange
    UUID auctionId1 = UUID.randomUUID();
    UUID auctionId2 = UUID.randomUUID();
    Auction auction1 = buildAuction(auctionId1, AuctionStatus.SCHEDULED);
    Auction auction2 = buildAuction(auctionId2, AuctionStatus.SCHEDULED);

    when(auctionRepository.findScheduledReadyToStart()).thenReturn(List.of(auction1, auction2));
    when(activateAuctionUseCase.run(new ActivateAuctionInput(auctionId1)))
        .thenReturn(new ActivateAuctionResult(auctionId1, AuctionStatus.ACTIVE));
    when(activateAuctionUseCase.run(new ActivateAuctionInput(auctionId2)))
        .thenReturn(new ActivateAuctionResult(auctionId2, AuctionStatus.ACTIVE));

    // act
    ProcessAuctionsResult result = useCase.run(NoInput.INSTANCE);

    // assert
    assertThat(result.processed()).isEqualTo(2);
    assertThat(result.failed()).isZero();
    verify(activateAuctionUseCase, times(2)).run(any());
  }

  // --- una falla, la otra se procesa (aislamiento de errores) ---

  @Test
  void execute_oneAuctionFailsToActivate_continuesAndCountsFailure() {
    // arrange
    UUID auctionId1 = UUID.randomUUID();
    UUID auctionId2 = UUID.randomUUID();
    Auction auction1 = buildAuction(auctionId1, AuctionStatus.SCHEDULED);
    Auction auction2 = buildAuction(auctionId2, AuctionStatus.SCHEDULED);

    when(auctionRepository.findScheduledReadyToStart()).thenReturn(List.of(auction1, auction2));
    when(activateAuctionUseCase.run(new ActivateAuctionInput(auctionId1)))
        .thenThrow(
            new InvalidAuctionStatusTransitionException(
                auctionId1, AuctionStatus.SCHEDULED, AuctionStatus.ACTIVE));
    when(activateAuctionUseCase.run(new ActivateAuctionInput(auctionId2)))
        .thenReturn(new ActivateAuctionResult(auctionId2, AuctionStatus.ACTIVE));

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
    Auction auction = buildAuction(auctionId, AuctionStatus.SCHEDULED);

    when(auctionRepository.findScheduledReadyToStart()).thenReturn(List.of(auction));
    when(activateAuctionUseCase.run(new ActivateAuctionInput(auctionId)))
        .thenThrow(new RuntimeException("boom"));

    // act
    ProcessAuctionsResult result = useCase.run(NoInput.INSTANCE);

    // assert
    assertThat(result.processed()).isZero();
    assertThat(result.failed()).isEqualTo(1);
  }
}
