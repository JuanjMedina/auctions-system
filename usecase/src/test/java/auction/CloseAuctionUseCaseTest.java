package auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import auction.input.CloseAuctionInput;
import auction.output.CloseAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionExceptions;
import domain.auction.AuctionExceptions.AuctionNotFoundException;
import domain.auction.AuctionExceptions.InvalidAuctionStatusTransitionException;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import domain.bid.Bid;
import domain.bid.BidRepository;
import domain.bid.BidStatus;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloseAuctionUseCaseTest {

  @Mock private AuctionRepository auctionRepository;
  @Mock private BidRepository bidRepository;
  @Mock private WalletRepository walletRepository;
  @Mock private OutboxEventRepository outboxEventRepository;

  @InjectMocks private CloseAuctionUseCase useCase;

  // --- fixtures ---

  private Auction buildAuction(
      UUID id,
      UUID sellerId,
      AuctionStatus status,
      BigDecimal currentPrice,
      BigDecimal reservePrice,
      UUID currentWinnerId) {
    return Auction.reconstitute(
        id,
        sellerId,
        UUID.randomUUID(),
        "Subasta test",
        "Descripcion",
        BigDecimal.TEN,
        reservePrice,
        currentPrice,
        currentWinnerId,
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

  private Bid buildActiveBid(UUID auctionId, UUID bidderId, BigDecimal amount) {
    return Bid.reconstitute(
        UUID.randomUUID(),
        auctionId,
        bidderId,
        amount,
        false,
        null,
        BidStatus.ACTIVE,
        Instant.now());
  }

  private Wallet buildWallet(UUID userId, BigDecimal balance, BigDecimal reservedBalance) {
    return Wallet.reconstitute(
        UUID.randomUUID(), userId, balance, reservedBalance, "USD", 0L, Instant.now());
  }

  // --- happy path: AWARDED ---

  @Test
  void execute_activeAuctionWithWinnerAndReserveMet_returnsAwardedStatus() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID winnerId = UUID.randomUUID();
    Auction auction =
        buildAuction(
            auctionId,
            sellerId,
            AuctionStatus.ACTIVE,
            BigDecimal.valueOf(20),
            BigDecimal.TEN,
            winnerId);
    Bid winningBid = buildActiveBid(auctionId, winnerId, BigDecimal.valueOf(20));
    Wallet winnerWallet = buildWallet(winnerId, BigDecimal.valueOf(20), BigDecimal.valueOf(20));
    Wallet sellerWallet = buildWallet(sellerId, BigDecimal.ZERO, BigDecimal.ZERO);

    when(auctionRepository.getById(auctionId)).thenReturn(auction);
    when(bidRepository.findActiveByAuctionId(auctionId)).thenReturn(List.of(winningBid));
    when(walletRepository.getByUserId(winnerId)).thenReturn(winnerWallet);
    when(walletRepository.getByUserId(sellerId)).thenReturn(sellerWallet);

    // act
    CloseAuctionResult result = useCase.run(new CloseAuctionInput(auctionId));

    // assert
    assertThat(result.auctionId()).isEqualTo(auctionId);
    assertThat(result.status()).isEqualTo(AuctionStatus.AWARDED);
    assertThat(result.winnerId()).isEqualTo(winnerId);
  }

  @Test
  void execute_awardedAuction_marksWinningBidAndChargesWinner() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID winnerId = UUID.randomUUID();
    Auction auction =
        buildAuction(
            auctionId,
            sellerId,
            AuctionStatus.ACTIVE,
            BigDecimal.valueOf(20),
            BigDecimal.TEN,
            winnerId);
    Bid winningBid = buildActiveBid(auctionId, winnerId, BigDecimal.valueOf(20));
    Wallet winnerWallet = buildWallet(winnerId, BigDecimal.valueOf(20), BigDecimal.valueOf(20));
    Wallet sellerWallet = buildWallet(sellerId, BigDecimal.ZERO, BigDecimal.ZERO);

    when(auctionRepository.getById(auctionId)).thenReturn(auction);
    when(bidRepository.findActiveByAuctionId(auctionId)).thenReturn(List.of(winningBid));
    when(walletRepository.getByUserId(winnerId)).thenReturn(winnerWallet);
    when(walletRepository.getByUserId(sellerId)).thenReturn(sellerWallet);

    // act
    useCase.run(new CloseAuctionInput(auctionId));

    // assert
    assertThat(winningBid.getStatus()).isEqualTo(BidStatus.WINNING);
    verify(walletRepository).save(winnerWallet);
    verify(walletRepository).save(sellerWallet);
    verify(walletRepository, times(2)).saveTransaction(any());
    verify(outboxEventRepository).save(any());
  }

  @Test
  void execute_awardedAuctionWithOtherActiveBids_releasesNonWinningBids() {
    // arrange: edge case: puja activa de otro bidder aun presente al cerrar
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID winnerId = UUID.randomUUID();
    UUID otherBidderId = UUID.randomUUID();
    Auction auction =
        buildAuction(
            auctionId,
            sellerId,
            AuctionStatus.ACTIVE,
            BigDecimal.valueOf(20),
            BigDecimal.TEN,
            winnerId);
    Bid winningBid = buildActiveBid(auctionId, winnerId, BigDecimal.valueOf(20));
    Bid otherBid = buildActiveBid(auctionId, otherBidderId, BigDecimal.valueOf(15));
    Wallet winnerWallet = buildWallet(winnerId, BigDecimal.valueOf(20), BigDecimal.valueOf(20));
    Wallet sellerWallet = buildWallet(sellerId, BigDecimal.ZERO, BigDecimal.ZERO);
    Wallet otherWallet = buildWallet(otherBidderId, BigDecimal.valueOf(15), BigDecimal.valueOf(15));

    when(auctionRepository.getById(auctionId)).thenReturn(auction);
    when(bidRepository.findActiveByAuctionId(auctionId)).thenReturn(List.of(winningBid, otherBid));
    when(walletRepository.getByUserId(winnerId)).thenReturn(winnerWallet);
    when(walletRepository.getByUserId(sellerId)).thenReturn(sellerWallet);
    when(walletRepository.getByUserId(otherBidderId)).thenReturn(otherWallet);

    // act
    useCase.run(new CloseAuctionInput(auctionId));

    // assert
    assertThat(winningBid.getStatus()).isEqualTo(BidStatus.WINNING);
    assertThat(otherBid.getStatus()).isEqualTo(BidStatus.CANCELLED);
    verify(bidRepository, times(2)).save(any(Bid.class));
  }

  // --- happy path: FAILED (sin ganador) ---

  @Test
  void execute_activeAuctionWithoutWinner_returnsFailedStatus() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    Auction auction =
        buildAuction(auctionId, sellerId, AuctionStatus.ACTIVE, BigDecimal.TEN, null, null);

    when(auctionRepository.getById(auctionId)).thenReturn(auction);
    when(bidRepository.findActiveByAuctionId(auctionId)).thenReturn(List.of());

    // act
    CloseAuctionResult result = useCase.run(new CloseAuctionInput(auctionId));

    // assert
    assertThat(result.status()).isEqualTo(AuctionStatus.FAILED);
    assertThat(result.winnerId()).isNull();
    verify(outboxEventRepository).save(any());
  }

  @Test
  void execute_activeAuctionReserveNotMet_returnsFailedStatusAndReleasesBids() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    Auction auction =
        buildAuction(
            auctionId,
            sellerId,
            AuctionStatus.ACTIVE,
            BigDecimal.valueOf(15),
            BigDecimal.valueOf(50), // reserve no alcanzado
            bidderId);
    Bid bid = buildActiveBid(auctionId, bidderId, BigDecimal.valueOf(15));
    Wallet wallet = buildWallet(bidderId, BigDecimal.valueOf(15), BigDecimal.valueOf(15));

    when(auctionRepository.getById(auctionId)).thenReturn(auction);
    when(bidRepository.findActiveByAuctionId(auctionId)).thenReturn(List.of(bid));
    when(walletRepository.getByUserId(bidderId)).thenReturn(wallet);

    // act
    CloseAuctionResult result = useCase.run(new CloseAuctionInput(auctionId));

    // assert
    assertThat(result.status()).isEqualTo(AuctionStatus.FAILED);
    assertThat(bid.getStatus()).isEqualTo(BidStatus.CANCELLED);
    verify(walletRepository).save(wallet);
    verify(walletRepository).saveTransaction(any());
  }

  // --- no encontrada ---

  @Test
  void execute_auctionNotFound_throwsAuctionNotFoundException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    when(auctionRepository.getById(auctionId))
        .thenThrow(new AuctionExceptions.AuctionNotFoundException(auctionId));

    // act & assert
    assertThatThrownBy(() -> useCase.run(new CloseAuctionInput(auctionId)))
        .isInstanceOf(AuctionNotFoundException.class);
  }

  // --- transicion invalida ---

  @Test
  void execute_alreadyClosedAuction_throwsInvalidAuctionStatusTransitionException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    Auction auction =
        buildAuction(auctionId, sellerId, AuctionStatus.CLOSED, BigDecimal.TEN, null, null);
    when(auctionRepository.getById(auctionId)).thenReturn(auction);

    // act & assert
    assertThatThrownBy(() -> useCase.run(new CloseAuctionInput(auctionId)))
        .isInstanceOf(InvalidAuctionStatusTransitionException.class);

    verify(bidRepository, never()).findActiveByAuctionId(any());
  }

  // --- wallet no encontrada ---

  @Test
  void execute_winnerWalletNotFound_throwsWalletNotFoundException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID winnerId = UUID.randomUUID();
    Auction auction =
        buildAuction(
            auctionId,
            sellerId,
            AuctionStatus.ACTIVE,
            BigDecimal.valueOf(20),
            BigDecimal.TEN,
            winnerId);
    Bid winningBid = buildActiveBid(auctionId, winnerId, BigDecimal.valueOf(20));

    when(auctionRepository.getById(auctionId)).thenReturn(auction);
    when(bidRepository.findActiveByAuctionId(auctionId)).thenReturn(List.of(winningBid));
    when(walletRepository.getByUserId(winnerId))
        .thenThrow(new WalletExceptions.WalletNotFoundException(winnerId));

    // act & assert
    assertThatThrownBy(() -> useCase.run(new CloseAuctionInput(auctionId)))
        .isInstanceOf(WalletNotFoundException.class);

    verify(auctionRepository, never()).save(any());
  }
}
