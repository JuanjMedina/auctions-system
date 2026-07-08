package auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import auction.input.ListMyAuctionsInput;
import auction.output.ListMyAuctionsResult;
import domain.auction.Auction;
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
class ListMyAuctionsUseCaseTest {

  @Mock private AuctionRepository auctionRepository;

  @InjectMocks private ListMyAuctionsUseCase useCase;

  // --- fixtures ---

  private Auction buildAuction(UUID sellerId, AuctionStatus status) {
    return Auction.reconstitute(
        UUID.randomUUID(),
        sellerId,
        UUID.randomUUID(),
        "Subasta test",
        "Descripcion",
        BigDecimal.TEN,
        BigDecimal.valueOf(20),
        BigDecimal.TEN,
        null,
        status,
        Instant.now(),
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
  void execute_sellerWithAuctions_returnsMappedSummaries() {
    // arrange
    UUID sellerId = UUID.randomUUID();
    Auction auction1 = buildAuction(sellerId, AuctionStatus.ACTIVE);
    Auction auction2 = buildAuction(sellerId, AuctionStatus.DRAFT);
    when(auctionRepository.findBySellerId(sellerId)).thenReturn(List.of(auction1, auction2));

    // act
    ListMyAuctionsResult result = useCase.run(new ListMyAuctionsInput(sellerId));

    // assert
    assertThat(result.auctions()).hasSize(2);
    assertThat(result.auctions()).allMatch(s -> s.sellerId().equals(sellerId));
  }

  @Test
  void execute_sellerWithAuctions_mapsFieldsCorrectly() {
    // arrange
    UUID sellerId = UUID.randomUUID();
    Auction auction = buildAuction(sellerId, AuctionStatus.ACTIVE);
    when(auctionRepository.findBySellerId(sellerId)).thenReturn(List.of(auction));

    // act
    ListMyAuctionsResult result = useCase.run(new ListMyAuctionsInput(sellerId));

    // assert
    assertThat(result.auctions()).hasSize(1);
    var summary = result.auctions().get(0);
    assertThat(summary.id()).isEqualTo(auction.getId());
    assertThat(summary.title()).isEqualTo(auction.getTitle());
    assertThat(summary.description()).isEqualTo(auction.getDescription());
    assertThat(summary.categoryId()).isEqualTo(auction.getCategoryId());
    assertThat(summary.currentPrice()).isEqualTo(auction.getCurrentPrice());
    assertThat(summary.status()).isEqualTo(AuctionStatus.ACTIVE);
    assertThat(summary.startsAt()).isEqualTo(auction.getStartsAt());
    assertThat(summary.endsAt()).isEqualTo(auction.getEndsAt());
  }

  // --- sin subastas ---

  @Test
  void execute_sellerWithoutAuctions_returnsEmptyList() {
    // arrange
    UUID sellerId = UUID.randomUUID();
    when(auctionRepository.findBySellerId(sellerId)).thenReturn(List.of());

    // act
    ListMyAuctionsResult result = useCase.run(new ListMyAuctionsInput(sellerId));

    // assert
    assertThat(result.auctions()).isEmpty();
  }
}
