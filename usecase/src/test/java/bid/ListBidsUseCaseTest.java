package bid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bid.input.ListBidsInput;
import bid.output.ListBidsResult;
import domain.auction.Auction;
import domain.auction.AuctionExceptions;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import domain.bid.Bid;
import domain.bid.BidRepository;
import domain.bid.BidStatus;
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
class ListBidsUseCaseTest {

  @Mock private AuctionRepository auctionRepository;
  @Mock private BidRepository bidRepository;

  @InjectMocks private ListBidsUseCase useCase;

  // --- fixtures ---
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
        5,
        List.of(),
        Instant.now(),
        Instant.now(),
        0L);
  }

  private Bid buildBid(BigDecimal amount, boolean autoBid, BidStatus status) {
    return Bid.reconstitute(
        UUID.randomUUID(),
        AUCTION_ID,
        UUID.randomUUID(),
        amount,
        autoBid,
        null,
        status,
        Instant.now());
  }

  // --- happy path ---

  @Test
  void execute_auctionWithBids_returnsBidsOrderedByAmountDesc() {
    // arrange
    Auction auction = buildAuction();
    List<Bid> bids =
        List.of(
            buildBid(BigDecimal.valueOf(30), false, BidStatus.WINNING),
            buildBid(BigDecimal.valueOf(20), false, BidStatus.OUTBID));

    when(auctionRepository.findById(AUCTION_ID)).thenReturn(Optional.of(auction));
    when(bidRepository.findByAuctionIdOrderByAmountDesc(AUCTION_ID)).thenReturn(bids);

    // act
    ListBidsResult result = useCase.run(new ListBidsInput(AUCTION_ID));

    // assert
    assertThat(result.bids()).hasSize(2);
    assertThat(result.bids().get(0).amount()).isEqualByComparingTo(BigDecimal.valueOf(30));
    assertThat(result.bids().get(1).amount()).isEqualByComparingTo(BigDecimal.valueOf(20));
  }

  @Test
  void execute_auctionWithBids_mapsFieldsCorrectly() {
    // arrange
    Auction auction = buildAuction();
    Bid bid = buildBid(BigDecimal.valueOf(30), true, BidStatus.WINNING);

    when(auctionRepository.findById(AUCTION_ID)).thenReturn(Optional.of(auction));
    when(bidRepository.findByAuctionIdOrderByAmountDesc(AUCTION_ID)).thenReturn(List.of(bid));

    // act
    ListBidsResult result = useCase.run(new ListBidsInput(AUCTION_ID));

    // assert
    ListBidsResult.BidSummary summary = result.bids().get(0);
    assertThat(summary.bidId()).isEqualTo(bid.getId());
    assertThat(summary.bidderId()).isEqualTo(bid.getBidderId());
    assertThat(summary.autoBid()).isTrue();
    assertThat(summary.status()).isEqualTo(BidStatus.WINNING);
    assertThat(summary.createdAt()).isEqualTo(bid.getCreatedAt());
  }

  // --- lista vacía ---

  @Test
  void execute_auctionWithNoBids_returnsEmptyList() {
    // arrange
    Auction auction = buildAuction();
    when(auctionRepository.findById(AUCTION_ID)).thenReturn(Optional.of(auction));
    when(bidRepository.findByAuctionIdOrderByAmountDesc(AUCTION_ID)).thenReturn(List.of());

    // act
    ListBidsResult result = useCase.run(new ListBidsInput(AUCTION_ID));

    // assert
    assertThat(result.bids()).isEmpty();
  }

  // --- subasta no encontrada ---

  @Test
  void execute_auctionNotFound_throwsAuctionNotFoundException() {
    // arrange
    when(auctionRepository.findById(AUCTION_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(new ListBidsInput(AUCTION_ID)))
        .isInstanceOf(AuctionExceptions.AuctionNotFoundException.class);
  }

  @Test
  void execute_auctionNotFound_neverQueriesBidRepository() {
    // arrange
    when(auctionRepository.findById(AUCTION_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(new ListBidsInput(AUCTION_ID)))
        .isInstanceOf(AuctionExceptions.AuctionNotFoundException.class);

    verify(bidRepository, never()).findByAuctionIdOrderByAmountDesc(AUCTION_ID);
  }
}
