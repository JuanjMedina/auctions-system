package bid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import bid.input.ListMyBidsInput;
import bid.output.ListMyBidsResult;
import domain.bid.Bid;
import domain.bid.BidRepository;
import domain.bid.BidStatus;
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
class ListMyBidsUseCaseTest {

  @Mock private BidRepository bidRepository;

  @InjectMocks private ListMyBidsUseCase useCase;

  // --- fixtures ---
  private static final UUID BIDDER_ID = UUID.randomUUID();

  private Bid buildBid(UUID auctionId, BigDecimal amount, boolean autoBid, BidStatus status) {
    return Bid.reconstitute(
        UUID.randomUUID(), auctionId, BIDDER_ID, amount, autoBid, null, status, Instant.now());
  }

  // --- happy path ---

  @Test
  void execute_bidderWithBids_returnsAllTheirBids() {
    // arrange
    UUID auction1 = UUID.randomUUID();
    UUID auction2 = UUID.randomUUID();
    List<Bid> bids =
        List.of(
            buildBid(auction1, BigDecimal.valueOf(20), false, BidStatus.OUTBID),
            buildBid(auction2, BigDecimal.valueOf(50), true, BidStatus.WINNING));

    when(bidRepository.findByBidderId(BIDDER_ID)).thenReturn(bids);

    // act
    ListMyBidsResult result = useCase.run(new ListMyBidsInput(BIDDER_ID));

    // assert
    assertThat(result.bids()).hasSize(2);
    assertThat(result.bids())
        .extracting(ListMyBidsResult.MyBidSummary::auctionId)
        .containsExactlyInAnyOrder(auction1, auction2);
  }

  @Test
  void execute_bidderWithBids_mapsFieldsCorrectly() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    Bid bid = buildBid(auctionId, BigDecimal.valueOf(50), true, BidStatus.WINNING);

    when(bidRepository.findByBidderId(BIDDER_ID)).thenReturn(List.of(bid));

    // act
    ListMyBidsResult result = useCase.run(new ListMyBidsInput(BIDDER_ID));

    // assert
    ListMyBidsResult.MyBidSummary summary = result.bids().get(0);
    assertThat(summary.bidId()).isEqualTo(bid.getId());
    assertThat(summary.auctionId()).isEqualTo(auctionId);
    assertThat(summary.amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
    assertThat(summary.autoBid()).isTrue();
    assertThat(summary.status()).isEqualTo(BidStatus.WINNING);
  }

  // --- lista vacía ---

  @Test
  void execute_bidderWithNoBids_returnsEmptyList() {
    // arrange
    when(bidRepository.findByBidderId(BIDDER_ID)).thenReturn(List.of());

    // act
    ListMyBidsResult result = useCase.run(new ListMyBidsInput(BIDDER_ID));

    // assert
    assertThat(result.bids()).isEmpty();
  }
}
