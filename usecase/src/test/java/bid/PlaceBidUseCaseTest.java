package bid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bid.input.PlaceBidInput;
import bid.output.PlaceBidOutput;
import domain.auction.Auction;
import domain.auction.AuctionExceptions;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import domain.bid.Bid;
import domain.bid.BidRepository;
import domain.bid.BidStatus;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import domain.wallets.Wallet;
import domain.wallets.WalletExceptions;
import domain.wallets.WalletRepository;
import domain.wallets.WalletTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceBidUseCaseTest {

  @Mock private AuctionRepository auctionRepository;
  @Mock private BidRepository bidRepository;
  @Mock private WalletRepository walletRepository;
  @Mock private OutboxEventRepository outboxEventRepository;

  @InjectMocks private PlaceBidUseCase useCase;

  // --- fixtures ---
  private static final UUID AUCTION_ID = UUID.randomUUID();
  private static final UUID SELLER_ID = UUID.randomUUID();
  private static final UUID BIDDER_ID = UUID.randomUUID();

  private Auction buildAuction() {
    return Auction.reconstitute(
        AUCTION_ID,
        SELLER_ID,
        UUID.randomUUID(),
        "Subasta test",
        "Descripcion",
        BigDecimal.TEN,
        null,
        BigDecimal.TEN,
        null,
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

  private Wallet buildWallet(UUID userId, BigDecimal balance) {
    return Wallet.reconstitute(
        UUID.randomUUID(), userId, balance, BigDecimal.ZERO, "USD", 0L, Instant.now());
  }

  private PlaceBidInput validInput() {
    return new PlaceBidInput(AUCTION_ID, BIDDER_ID, BigDecimal.valueOf(20), false, null);
  }

  private void mockSaves() {
    when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.saveTransaction(any())).thenAnswer(inv -> inv.getArgument(0));
    when(auctionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
    when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  // --- happy path ---

  @Test
  void execute_validBid_returnsPlaceBidOutput() {
    // arrange
    Auction auction = buildAuction();
    Wallet wallet = buildWallet(BIDDER_ID, BigDecimal.valueOf(100));

    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(wallet);
    mockSaves();

    // act
    PlaceBidOutput result = useCase.run(validInput());

    // assert
    assertThat(result.auctionId()).isEqualTo(AUCTION_ID);
    assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(20));
    assertThat(result.status()).isEqualTo(BidStatus.ACTIVE);
    assertThat(result.bidId()).isNotNull();
  }

  @Test
  void execute_validBid_reservesFundsAndPersistsBid() {
    // arrange
    Auction auction = buildAuction();
    Wallet wallet = buildWallet(BIDDER_ID, BigDecimal.valueOf(100));

    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(wallet);
    mockSaves();

    // act
    useCase.run(validInput());

    // assert
    verify(walletRepository).save(wallet);
    verify(walletRepository).saveTransaction(any(WalletTransaction.class));
    verify(auctionRepository).save(auction);
    verify(bidRepository).save(any(Bid.class));
    verify(outboxEventRepository).save(any(OutboxEvent.class));
  }

  @Test
  void execute_previousWinnerExists_releasesTheirFunds() {
    // arrange
    UUID previousWinnerId = UUID.randomUUID();
    Auction auction =
        Auction.reconstitute(
            AUCTION_ID,
            SELLER_ID,
            UUID.randomUUID(),
            "Subasta test",
            "Descripcion",
            BigDecimal.TEN,
            null,
            BigDecimal.valueOf(15),
            previousWinnerId,
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
    Wallet bidderWallet = buildWallet(BIDDER_ID, BigDecimal.valueOf(100));
    Wallet previousWallet = buildWallet(previousWinnerId, BigDecimal.valueOf(50));
    previousWallet.reserve(BigDecimal.valueOf(15), UUID.randomUUID());

    Bid previousBid =
        Bid.reconstitute(
            UUID.randomUUID(),
            AUCTION_ID,
            previousWinnerId,
            BigDecimal.valueOf(15),
            false,
            null,
            BidStatus.ACTIVE,
            Instant.now());

    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(bidderWallet);
    when(bidRepository.findLatestActiveBidByAuctionIdAndBidderId(AUCTION_ID, previousWinnerId))
        .thenReturn(Optional.of(previousBid));
    when(walletRepository.getByUserId(previousWinnerId)).thenReturn(previousWallet);
    mockSaves();

    // act
    useCase.run(validInput());

    // assert
    verify(bidRepository).save(previousBid);
    assertThat(previousBid.getStatus()).isEqualTo(BidStatus.OUTBID);
    verify(walletRepository).save(previousWallet);
  }

  // --- subasta no encontrada ---

  @Test
  void execute_auctionNotFound_throwsAuctionNotFoundException() {
    // arrange
    when(auctionRepository.getById(AUCTION_ID))
        .thenThrow(new AuctionExceptions.AuctionNotFoundException(AUCTION_ID));

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(AuctionExceptions.AuctionNotFoundException.class);
  }

  @Test
  void execute_auctionNotFound_neverTouchesWalletOrBidRepositories() {
    // arrange
    when(auctionRepository.getById(AUCTION_ID))
        .thenThrow(new AuctionExceptions.AuctionNotFoundException(AUCTION_ID));

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(AuctionExceptions.AuctionNotFoundException.class);

    verify(walletRepository, never()).getByUserId(any());
    verify(bidRepository, never()).save(any());
  }

  // --- billetera no encontrada ---

  @Test
  void execute_walletNotFound_throwsWalletNotFoundException() {
    // arrange
    Auction auction = buildAuction();
    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID))
        .thenThrow(new WalletExceptions.WalletNotFoundException(BIDDER_ID));

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WalletExceptions.WalletNotFoundException.class);
  }

  // --- fondos insuficientes ---

  @Test
  void execute_insufficientFunds_throwsInsufficientFundsException() {
    // arrange
    Auction auction = buildAuction();
    Wallet wallet = buildWallet(BIDDER_ID, BigDecimal.valueOf(5));

    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(wallet);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WalletExceptions.InsufficientFundsException.class);
  }

  @Test
  void execute_insufficientFunds_neverPlacesBidOrSaves() {
    // arrange
    Auction auction = buildAuction();
    Wallet wallet = buildWallet(BIDDER_ID, BigDecimal.valueOf(5));

    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(wallet);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WalletExceptions.InsufficientFundsException.class);

    verify(bidRepository, never()).save(any());
    verify(auctionRepository, never()).save(any());
    verify(outboxEventRepository, never()).save(any());
  }

  // --- vendedor no puede pujar en su propia subasta ---

  @Test
  void execute_sellerBidsOnOwnAuction_throwsSellerCannotBidException() {
    // arrange
    Auction auction = buildAuction();
    Wallet wallet = buildWallet(SELLER_ID, BigDecimal.valueOf(100));

    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(SELLER_ID)).thenReturn(wallet);

    PlaceBidInput input =
        new PlaceBidInput(AUCTION_ID, SELLER_ID, BigDecimal.valueOf(20), false, null);

    // act & assert
    assertThatThrownBy(() -> useCase.run(input))
        .isInstanceOf(AuctionExceptions.SellerCannotBidException.class);
  }

  // --- puja demasiado baja ---

  @Test
  void execute_bidTooLow_throwsBidTooLowException() {
    // arrange
    Auction auction = buildAuction();
    Wallet wallet = buildWallet(BIDDER_ID, BigDecimal.valueOf(100));

    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(wallet);

    PlaceBidInput input =
        new PlaceBidInput(AUCTION_ID, BIDDER_ID, BigDecimal.valueOf(5), false, null);

    // act & assert
    assertThatThrownBy(() -> useCase.run(input))
        .isInstanceOf(AuctionExceptions.BidTooLowException.class);
  }

  // --- subasta no activa ---

  @Test
  void execute_auctionNotActive_throwsAuctionNotActiveException() {
    // arrange
    Auction closedAuction =
        Auction.reconstitute(
            AUCTION_ID,
            SELLER_ID,
            UUID.randomUUID(),
            "Subasta test",
            "Descripcion",
            BigDecimal.TEN,
            null,
            BigDecimal.TEN,
            null,
            AuctionStatus.CLOSED,
            Instant.now().minusSeconds(7200),
            Instant.now().minusSeconds(3600),
            Instant.now(),
            false,
            5,
            List.of(),
            Instant.now(),
            Instant.now(),
            0L);
    Wallet wallet = buildWallet(BIDDER_ID, BigDecimal.valueOf(100));

    when(auctionRepository.getById(AUCTION_ID)).thenReturn(closedAuction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(wallet);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(AuctionExceptions.AuctionNotActiveException.class);
  }

  // --- eventos de outbox ---

  @Test
  void execute_previousWinnerExists_emitsBidOutbidAndBidPlacedEvents() {
    // arrange
    UUID previousWinnerId = UUID.randomUUID();
    Auction auction =
        Auction.reconstitute(
            AUCTION_ID,
            SELLER_ID,
            UUID.randomUUID(),
            "Subasta test",
            "Descripcion",
            BigDecimal.TEN,
            null,
            BigDecimal.valueOf(15),
            previousWinnerId,
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
    Wallet bidderWallet = buildWallet(BIDDER_ID, BigDecimal.valueOf(100));
    Wallet previousWallet = buildWallet(previousWinnerId, BigDecimal.valueOf(50));
    previousWallet.reserve(BigDecimal.valueOf(15), UUID.randomUUID());
    Bid previousBid =
        Bid.reconstitute(
            UUID.randomUUID(),
            AUCTION_ID,
            previousWinnerId,
            BigDecimal.valueOf(15),
            false,
            null,
            BidStatus.ACTIVE,
            Instant.now());

    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(bidderWallet);
    when(bidRepository.findLatestActiveBidByAuctionIdAndBidderId(AUCTION_ID, previousWinnerId))
        .thenReturn(Optional.of(previousBid));
    when(walletRepository.getByUserId(previousWinnerId)).thenReturn(previousWallet);
    mockSaves();

    // act
    useCase.run(validInput());

    // assert
    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(OutboxEvent::getEventType)
        .containsExactly(EventType.BID_OUTBID, EventType.BID_PLACED);
    assertThat(captor.getAllValues().get(0).getAggregateId()).isEqualTo(previousBid.getId());
  }

  @Test
  void execute_noPreviousWinner_emitsOnlyBidPlacedEvent() {
    // arrange
    Auction auction = buildAuction();
    Wallet wallet = buildWallet(BIDDER_ID, BigDecimal.valueOf(100));
    when(auctionRepository.getById(AUCTION_ID)).thenReturn(auction);
    when(walletRepository.getByUserId(BIDDER_ID)).thenReturn(wallet);
    mockSaves();

    // act
    useCase.run(validInput());

    // assert
    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    assertThat(captor.getValue().getEventType()).isEqualTo(EventType.BID_PLACED);
  }
}
