package auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import auction.input.GetAuctionInput;
import auction.output.GetAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionExceptions;
import domain.auction.AuctionExceptions.AuctionNotFoundException;
import domain.auction.AuctionImage;
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

@ExtendWith(MockitoExtension.class)
class GetAuctionUseCaseTest {

  @Mock private AuctionRepository auctionRepository;

  @InjectMocks private GetAuctionUseCase useCase;

  // --- fixtures ---

  private Auction buildAuction(UUID id, List<AuctionImage> images) {
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
        AuctionStatus.ACTIVE,
        Instant.now(),
        Instant.now().plusSeconds(3600),
        null,
        false,
        5,
        images,
        Instant.now(),
        Instant.now(),
        0L);
  }

  // --- happy path ---

  @Test
  void execute_existingAuction_returnsMappedResult() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, List.of());
    when(auctionRepository.getById(auctionId)).thenReturn(auction);

    // act
    GetAuctionResult result = useCase.run(new GetAuctionInput(auctionId));

    // assert
    assertThat(result.id()).isEqualTo(auctionId);
    assertThat(result.sellerId()).isEqualTo(auction.getSellerId());
    assertThat(result.categoryId()).isEqualTo(auction.getCategoryId());
    assertThat(result.title()).isEqualTo("Subasta test");
    assertThat(result.status()).isEqualTo(AuctionStatus.ACTIVE);
    assertThat(result.images()).isEmpty();
  }

  @Test
  void execute_auctionWithImages_mapsImagesCorrectly() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    AuctionImage image1 = new AuctionImage(UUID.randomUUID(), "http://img1.png", true, 0);
    AuctionImage image2 = new AuctionImage(UUID.randomUUID(), "http://img2.png", false, 1);
    Auction auction = buildAuction(auctionId, List.of(image1, image2));
    when(auctionRepository.getById(auctionId)).thenReturn(auction);

    // act
    GetAuctionResult result = useCase.run(new GetAuctionInput(auctionId));

    // assert
    assertThat(result.images()).hasSize(2);
    assertThat(result.images().get(0).url()).isEqualTo("http://img1.png");
    assertThat(result.images().get(0).primary()).isTrue();
    assertThat(result.images().get(1).url()).isEqualTo("http://img2.png");
    assertThat(result.images().get(1).displayOrder()).isEqualTo(1);
  }

  // --- no encontrada ---

  @Test
  void execute_auctionNotFound_throwsAuctionNotFoundException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    when(auctionRepository.getById(auctionId))
        .thenThrow(new AuctionExceptions.AuctionNotFoundException(auctionId));

    // act & assert
    assertThatThrownBy(() -> useCase.run(new GetAuctionInput(auctionId)))
        .isInstanceOf(AuctionNotFoundException.class);
  }
}
