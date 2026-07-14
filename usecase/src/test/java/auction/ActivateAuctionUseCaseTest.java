package auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import auction.input.ActivateAuctionInput;
import auction.output.ActivateAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionExceptions;
import domain.auction.AuctionExceptions.AuctionNotFoundException;
import domain.auction.AuctionExceptions.InvalidAuctionStatusTransitionException;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import domain.outbox.AggregateType;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivateAuctionUseCaseTest {

  @Mock private AuctionRepository auctionRepository;
  @Mock private OutboxEventRepository outboxEventRepository;

  @InjectMocks private ActivateAuctionUseCase useCase;

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

  // --- happy path ---

  @Test
  void execute_scheduledAuction_returnsActiveStatus() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, AuctionStatus.SCHEDULED);
    when(auctionRepository.getById(auctionId)).thenReturn(auction);
    when(auctionRepository.save(auction)).thenReturn(auction);

    // act
    ActivateAuctionResult result = useCase.run(new ActivateAuctionInput(auctionId));

    // assert
    assertThat(result.id()).isEqualTo(auctionId);
    assertThat(result.status()).isEqualTo(AuctionStatus.ACTIVE);
  }

  @Test
  void execute_scheduledAuction_savesAuction() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, AuctionStatus.SCHEDULED);
    when(auctionRepository.getById(auctionId)).thenReturn(auction);
    when(auctionRepository.save(auction)).thenReturn(auction);

    // act
    useCase.run(new ActivateAuctionInput(auctionId));

    // assert
    verify(auctionRepository).save(auction);
  }

  // --- no encontrada ---

  @Test
  void execute_auctionNotFound_throwsAuctionNotFoundException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    when(auctionRepository.getById(auctionId))
        .thenThrow(new AuctionExceptions.AuctionNotFoundException(auctionId));

    // act & assert
    assertThatThrownBy(() -> useCase.run(new ActivateAuctionInput(auctionId)))
        .isInstanceOf(AuctionNotFoundException.class);
  }

  // --- transicion invalida ---

  @Test
  void execute_draftAuction_throwsInvalidAuctionStatusTransitionException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, AuctionStatus.DRAFT);
    when(auctionRepository.getById(auctionId)).thenReturn(auction);

    // act & assert
    assertThatThrownBy(() -> useCase.run(new ActivateAuctionInput(auctionId)))
        .isInstanceOf(InvalidAuctionStatusTransitionException.class);
  }

  @Test
  void execute_alreadyActiveAuction_throwsInvalidAuctionStatusTransitionException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, AuctionStatus.ACTIVE);
    when(auctionRepository.getById(auctionId)).thenReturn(auction);

    // act & assert
    assertThatThrownBy(() -> useCase.run(new ActivateAuctionInput(auctionId)))
        .isInstanceOf(InvalidAuctionStatusTransitionException.class);
  }

  // --- evento de outbox ---

  @Test
  void execute_scheduledAuction_emitsAuctionActivatedEvent() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, AuctionStatus.SCHEDULED);
    when(auctionRepository.getById(auctionId)).thenReturn(auction);
    when(auctionRepository.save(auction)).thenReturn(auction);

    // act
    useCase.run(new ActivateAuctionInput(auctionId));

    // assert
    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    OutboxEvent event = captor.getValue();
    assertThat(event.getEventType()).isEqualTo(EventType.AUCTION_ACTIVATED);
    assertThat(event.getAggregateType()).isEqualTo(AggregateType.AUCTION);
    assertThat(event.getAggregateId()).isEqualTo(auctionId);
  }
}
