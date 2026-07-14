package bid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bid.input.DeleteBidInput;
import bid.output.DeleteBidOutput;
import domain.auction.Auction;
import domain.auction.AuctionExceptions;
import domain.auction.AuctionExceptions.AuctionNotFoundException;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import domain.bid.Bid;
import domain.bid.BidExceptions;
import domain.bid.BidExceptions.BidNotFoundException;
import domain.bid.BidExceptions.InvalidBidStatusTransitionException;
import domain.bid.BidExceptions.UnauthorizedBidAccessException;
import domain.bid.BidRepository;
import domain.bid.BidStatus;
import domain.outbox.AggregateType;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import domain.wallets.Wallet;
import domain.wallets.WalletExceptions;
import domain.wallets.WalletExceptions.WalletNotFoundException;
import domain.wallets.WalletRepository;
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
class DeleteBidUseCaseTest {

  @Mock private BidRepository bidRepository;
  @Mock private AuctionRepository auctionRepository;
  @Mock private WalletRepository walletRepository;
  @Mock private OutboxEventRepository outboxEventRepository;

  @InjectMocks private DeleteBidUseCase useCase;

  // --- fixtures ---
  private static final UUID AUCTION_ID = UUID.randomUUID();
  private static final UUID SELLER_ID = UUID.randomUUID();
  private static final UUID BIDDER_ID = UUID.randomUUID();

  private Auction buildAuction(BigDecimal currentPrice, UUID currentWinnerId) {
    return Auction.reconstitute(
        AUCTION_ID,
        SELLER_ID,
        UUID.randomUUID(),
        "Subasta test",
        "Descripcion",
        BigDecimal.TEN,
        null,
        currentPrice,
        currentWinnerId,
        AuctionStatus.ACTIVE,
        Instant.now().minusSeconds(3600),
        Instant.now().plusSeconds(3600),
        null,
        false,
        5,
        List.of(),
        Instant.now(),
        Instant.now(),
        0L);
  }

  private Bid buildBid(UUID bidderId, BigDecimal amount, BidStatus status) {
    return Bid.reconstitute(
        UUID.randomUUID(), AUCTION_ID, bidderId, amount, false, null, status, Instant.now());
  }

  private Wallet buildWalletWithReserve(UUID userId, BigDecimal reservedAmount) {
    return Wallet.reconstitute(
        UUID.randomUUID(), userId, reservedAmount, reservedAmount, "USD", 0L, Instant.now());
  }

  private void mockSaves() {
    when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
    when(auctionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.saveTransaction(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  // --- happy path: puja cancelada no es la lider ---

  @Test
  void execute_notTheLeader_cancelsBidAndReleasesFundsWithoutTouchingAuction() {
    Bid bid = buildBid(BIDDER_ID, BigDecimal.TEN, BidStatus.ACTIVE);
    Bid leaderBid = buildBid(UUID.randomUUID(), BigDecimal.valueOf(20), BidStatus.ACTIVE);
    Auction auction = buildAuction(BigDecimal.valueOf(20), leaderBid.getBidderId());
    Wallet wallet = buildWalletWithReserve(BIDDER_ID, BigDecimal.TEN);

    when(bidRepository.getById(bid.getId())).thenReturn(bid);
    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(wallet);
    when(bidRepository.findActiveByAuctionId(AUCTION_ID)).thenReturn(List.of(leaderBid));
    when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.saveTransaction(any())).thenAnswer(inv -> inv.getArgument(0));

    DeleteBidOutput result = useCase.run(new DeleteBidInput(bid.getId(), AUCTION_ID, BIDDER_ID));

    assertThat(result.status()).isEqualTo(BidStatus.CANCELLED);
    assertThat(bid.getStatus()).isEqualTo(BidStatus.CANCELLED);
    verify(walletRepository).save(wallet);
    verify(walletRepository).saveTransaction(any());
    verify(bidRepository).save(bid);
    verify(auctionRepository, never()).save(any());
  }

  // --- happy path: era la lider, hay una puja OUTBID para promover ---

  @Test
  void execute_wasLeaderWithOutbidCandidate_promotesNextBestBid() {
    Bid bid = buildBid(BIDDER_ID, BigDecimal.valueOf(20), BidStatus.ACTIVE);
    UUID nextBidderId = UUID.randomUUID();
    Bid outbidCandidate = buildBid(nextBidderId, BigDecimal.TEN, BidStatus.OUTBID);
    Auction auction = buildAuction(BigDecimal.valueOf(20), BIDDER_ID);
    Wallet wallet = buildWalletWithReserve(BIDDER_ID, BigDecimal.valueOf(20));

    when(bidRepository.getById(bid.getId())).thenReturn(bid);
    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(wallet);
    when(bidRepository.findActiveByAuctionId(AUCTION_ID)).thenReturn(List.of(bid));
    when(bidRepository.findByAuctionIdOrderByAmountDesc(AUCTION_ID))
        .thenReturn(List.of(bid, outbidCandidate));
    mockSaves();

    useCase.run(new DeleteBidInput(bid.getId(), AUCTION_ID, BIDDER_ID));

    assertThat(outbidCandidate.getStatus()).isEqualTo(BidStatus.ACTIVE);
    assertThat(auction.getCurrentPrice()).isEqualByComparingTo(BigDecimal.TEN);
    assertThat(auction.getCurrentWinnerId()).isEqualTo(nextBidderId);
    verify(bidRepository).save(outbidCandidate);
    verify(auctionRepository).save(auction);
  }

  // --- happy path: era la lider, no hay otras pujas ---

  @Test
  void execute_wasLeaderWithoutOtherBids_resetsAuctionToNoWinner() {
    Bid bid = buildBid(BIDDER_ID, BigDecimal.valueOf(20), BidStatus.ACTIVE);
    Auction auction = buildAuction(BigDecimal.valueOf(20), BIDDER_ID);
    Wallet wallet = buildWalletWithReserve(BIDDER_ID, BigDecimal.valueOf(20));

    when(bidRepository.getById(bid.getId())).thenReturn(bid);
    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(wallet);
    when(bidRepository.findActiveByAuctionId(AUCTION_ID)).thenReturn(List.of(bid));
    when(bidRepository.findByAuctionIdOrderByAmountDesc(AUCTION_ID)).thenReturn(List.of(bid));
    mockSaves();

    useCase.run(new DeleteBidInput(bid.getId(), AUCTION_ID, BIDDER_ID));

    assertThat(auction.getCurrentPrice()).isEqualByComparingTo(BigDecimal.TEN); // startingPrice
    assertThat(auction.getCurrentWinnerId()).isNull();
    verify(auctionRepository).save(auction);
  }

  // --- puja no encontrada ---

  @Test
  void execute_bidNotFound_throwsBidNotFoundException() {
    UUID bidId = UUID.randomUUID();
    when(bidRepository.getById(bidId)).thenThrow(new BidExceptions.BidNotFoundException(bidId));

    assertThatThrownBy(() -> useCase.run(new DeleteBidInput(bidId, AUCTION_ID, BIDDER_ID)))
        .isInstanceOf(BidNotFoundException.class);
  }

  // --- la puja no pertenece a la subasta indicada ---

  @Test
  void execute_bidBelongsToDifferentAuction_throwsBidNotFoundException() {
    Bid bid = buildBid(BIDDER_ID, BigDecimal.TEN, BidStatus.ACTIVE);
    when(bidRepository.getById(bid.getId())).thenReturn(bid);

    UUID otherAuctionId = UUID.randomUUID();

    assertThatThrownBy(
            () -> useCase.run(new DeleteBidInput(bid.getId(), otherAuctionId, BIDDER_ID)))
        .isInstanceOf(BidNotFoundException.class);

    verify(auctionRepository, never()).getById(any());
  }

  // --- no es el dueño de la puja ---

  @Test
  void execute_notTheOwner_throwsUnauthorizedBidAccessException() {
    Bid bid = buildBid(BIDDER_ID, BigDecimal.TEN, BidStatus.ACTIVE);
    when(bidRepository.getById(bid.getId())).thenReturn(bid);

    UUID otherUserId = UUID.randomUUID();

    assertThatThrownBy(() -> useCase.run(new DeleteBidInput(bid.getId(), AUCTION_ID, otherUserId)))
        .isInstanceOf(UnauthorizedBidAccessException.class);

    verify(auctionRepository, never()).getById(any());
    verify(walletRepository, never()).getByUserId(any());
  }

  // --- la puja no esta activa ---

  @Test
  void execute_bidNotActive_throwsInvalidBidStatusTransitionException() {
    Bid bid = buildBid(BIDDER_ID, BigDecimal.TEN, BidStatus.CANCELLED);
    when(bidRepository.getById(bid.getId())).thenReturn(bid);

    assertThatThrownBy(() -> useCase.run(new DeleteBidInput(bid.getId(), AUCTION_ID, BIDDER_ID)))
        .isInstanceOf(InvalidBidStatusTransitionException.class);

    verify(walletRepository, never()).getByUserId(any());
  }

  // --- subasta no encontrada ---

  @Test
  void execute_auctionNotFound_throwsAuctionNotFoundException() {
    Bid bid = buildBid(BIDDER_ID, BigDecimal.TEN, BidStatus.ACTIVE);
    when(bidRepository.getById(bid.getId())).thenReturn(bid);
    when(auctionRepository.getById(AUCTION_ID))
        .thenThrow(new AuctionExceptions.AuctionNotFoundException(AUCTION_ID));

    assertThatThrownBy(() -> useCase.run(new DeleteBidInput(bid.getId(), AUCTION_ID, BIDDER_ID)))
        .isInstanceOf(AuctionNotFoundException.class);

    verify(walletRepository, never()).getByUserId(any());
  }

  // --- billetera no encontrada ---

  @Test
  void execute_walletNotFound_throwsWalletNotFoundException() {
    Bid bid = buildBid(BIDDER_ID, BigDecimal.TEN, BidStatus.ACTIVE);
    Auction auction = buildAuction(BigDecimal.TEN, BIDDER_ID);
    when(bidRepository.getById(bid.getId())).thenReturn(bid);
    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID))
        .thenThrow(new WalletExceptions.WalletNotFoundException(BIDDER_ID));

    assertThatThrownBy(() -> useCase.run(new DeleteBidInput(bid.getId(), AUCTION_ID, BIDDER_ID)))
        .isInstanceOf(WalletNotFoundException.class);

    verify(bidRepository, never()).save(any());
    verify(auctionRepository, never()).save(any());
  }

  // --- evento de outbox ---

  @Test
  void execute_cancelledBid_emitsBidCancelledEvent() {
    Bid bid = buildBid(BIDDER_ID, BigDecimal.TEN, BidStatus.ACTIVE);
    Bid leaderBid = buildBid(UUID.randomUUID(), BigDecimal.valueOf(20), BidStatus.ACTIVE);
    Auction auction = buildAuction(BigDecimal.valueOf(20), leaderBid.getBidderId());
    Wallet wallet = buildWalletWithReserve(BIDDER_ID, BigDecimal.TEN);

    when(bidRepository.getById(bid.getId())).thenReturn(bid);
    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(wallet);
    when(bidRepository.findActiveByAuctionId(AUCTION_ID)).thenReturn(List.of(leaderBid));
    when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.saveTransaction(any())).thenAnswer(inv -> inv.getArgument(0));

    useCase.run(new DeleteBidInput(bid.getId(), AUCTION_ID, BIDDER_ID));

    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    OutboxEvent event = captor.getValue();
    assertThat(event.getEventType()).isEqualTo(EventType.BID_CANCELLED);
    assertThat(event.getAggregateType()).isEqualTo(AggregateType.BID);
    assertThat(event.getAggregateId()).isEqualTo(bid.getId());
  }
}
