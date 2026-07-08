package auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import auction.input.PublishAuctionInput;
import auction.output.PublishAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionExceptions.AuctionNotFoundException;
import domain.auction.AuctionExceptions.InvalidAuctionStatusTransitionException;
import domain.auction.AuctionExceptions.UnauthorizedAuctionAccessException;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
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

@ExtendWith(MockitoExtension.class)
class PublishAuctionUseCaseTest {

  @Mock private AuctionRepository auctionRepository;

  @InjectMocks private PublishAuctionUseCase useCase;

  // --- fixtures ---

  private Auction buildAuction(UUID id, UUID sellerId, AuctionStatus status, Instant startsAt) {
    return Auction.reconstitute(
        id,
        sellerId,
        UUID.randomUUID(),
        "Subasta test",
        "Descripcion",
        BigDecimal.TEN,
        BigDecimal.valueOf(20),
        BigDecimal.TEN,
        null,
        status,
        startsAt,
        startsAt.plusSeconds(3600),
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
  void execute_draftAuctionFutureStart_returnsScheduledStatus() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    Auction auction =
        buildAuction(auctionId, sellerId, AuctionStatus.DRAFT, Instant.now().plusSeconds(3600));
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
    when(auctionRepository.save(auction)).thenReturn(auction);

    // act
    PublishAuctionResult result = useCase.run(new PublishAuctionInput(auctionId, sellerId));

    // assert
    assertThat(result.id()).isEqualTo(auctionId);
    assertThat(result.status()).isEqualTo(AuctionStatus.SCHEDULED);
  }

  @Test
  void execute_draftAuctionStartInPast_returnsActiveStatus() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    Auction auction =
        buildAuction(auctionId, sellerId, AuctionStatus.DRAFT, Instant.now().minusSeconds(3600));
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
    when(auctionRepository.save(auction)).thenReturn(auction);

    // act
    PublishAuctionResult result = useCase.run(new PublishAuctionInput(auctionId, sellerId));

    // assert
    assertThat(result.status()).isEqualTo(AuctionStatus.ACTIVE);
  }

  @Test
  void execute_validPublish_savesAuction() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    Auction auction =
        buildAuction(auctionId, sellerId, AuctionStatus.DRAFT, Instant.now().plusSeconds(3600));
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
    when(auctionRepository.save(auction)).thenReturn(auction);

    // act
    useCase.run(new PublishAuctionInput(auctionId, sellerId));

    // assert
    verify(auctionRepository).save(auction);
  }

  // --- no encontrada ---

  @Test
  void execute_auctionNotFound_throwsAuctionNotFoundException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(new PublishAuctionInput(auctionId, sellerId)))
        .isInstanceOf(AuctionNotFoundException.class);
  }

  // --- no autorizado ---

  @Test
  void execute_notOwner_throwsUnauthorizedAuctionAccessException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    Auction auction =
        buildAuction(auctionId, sellerId, AuctionStatus.DRAFT, Instant.now().plusSeconds(3600));
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

    // act & assert
    assertThatThrownBy(() -> useCase.run(new PublishAuctionInput(auctionId, otherUserId)))
        .isInstanceOf(UnauthorizedAuctionAccessException.class);
  }

  // --- transicion invalida ---

  @Test
  void execute_alreadyPublishedAuction_throwsInvalidAuctionStatusTransitionException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    Auction auction =
        buildAuction(auctionId, sellerId, AuctionStatus.ACTIVE, Instant.now().minusSeconds(3600));
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

    // act & assert
    assertThatThrownBy(() -> useCase.run(new PublishAuctionInput(auctionId, sellerId)))
        .isInstanceOf(InvalidAuctionStatusTransitionException.class);
  }
}
