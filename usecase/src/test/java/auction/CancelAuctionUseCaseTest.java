package auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import auction.input.CancelAuctionInput;
import auction.output.CancelAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionExceptions.AuctionNotFoundException;
import domain.auction.AuctionExceptions.InvalidAuctionStatusTransitionException;
import domain.auction.AuctionExceptions.UnauthorizedAuctionAccessException;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import domain.bid.Bid;
import domain.bid.BidRepository;
import domain.bid.BidStatus;
import domain.outbox.OutboxEventRepository;
import domain.wallets.Wallet;
import domain.wallets.WalletExceptions.WalletNotFoundException;
import domain.wallets.WalletRepository;
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
class CancelAuctionUseCaseTest {

  @Mock private AuctionRepository auctionRepository;
  @Mock private BidRepository bidRepository;
  @Mock private WalletRepository walletRepository;
  @Mock private OutboxEventRepository outboxEventRepository;

  @InjectMocks private CancelAuctionUseCase useCase;

  // --- fixtures ---

  private Auction buildAuction(UUID id, UUID sellerId, AuctionStatus status) {
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

  private Wallet buildWallet(UUID userId, BigDecimal reservedBalance) {
    return Wallet.reconstitute(
        UUID.randomUUID(), userId, reservedBalance, reservedBalance, "USD", 0L, Instant.now());
  }

  // --- happy path: sin pujas activas ---

  @Test
  void execute_noActiveBids_cancelsAuctionWithZeroReleasedBids() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, sellerId, AuctionStatus.ACTIVE);
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
    when(bidRepository.findActiveByAuctionId(auctionId)).thenReturn(List.of());

    // act
    CancelAuctionResult result = useCase.run(new CancelAuctionInput(auctionId, sellerId));

    // assert
    assertThat(result.auctionId()).isEqualTo(auctionId);
    assertThat(result.status()).isEqualTo(AuctionStatus.CANCELLED);
    assertThat(result.releasedBids()).isZero();
    verifyNoInteractions(walletRepository);
  }

  // --- happy path: con pujas activas ---

  @Test
  void execute_withActiveBids_releasesReservesAndCancelsBids() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, sellerId, AuctionStatus.ACTIVE);
    Bid bid = buildActiveBid(auctionId, bidderId, BigDecimal.TEN);
    Wallet wallet = buildWallet(bidderId, BigDecimal.TEN);

    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
    when(bidRepository.findActiveByAuctionId(auctionId)).thenReturn(List.of(bid));
    when(walletRepository.findByUserId(bidderId)).thenReturn(Optional.of(wallet));

    // act
    CancelAuctionResult result = useCase.run(new CancelAuctionInput(auctionId, sellerId));

    // assert
    assertThat(result.releasedBids()).isEqualTo(1);
    assertThat(bid.getStatus()).isEqualTo(BidStatus.CANCELLED);
    verify(walletRepository).save(wallet);
    verify(walletRepository).saveTransaction(any());
    verify(bidRepository).save(bid);
  }

  @Test
  void execute_withMultipleActiveBids_releasesAllReserves() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID bidder1 = UUID.randomUUID();
    UUID bidder2 = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, sellerId, AuctionStatus.ACTIVE);
    Bid bid1 = buildActiveBid(auctionId, bidder1, BigDecimal.TEN);
    Bid bid2 = buildActiveBid(auctionId, bidder2, BigDecimal.valueOf(15));
    Wallet wallet1 = buildWallet(bidder1, BigDecimal.TEN);
    Wallet wallet2 = buildWallet(bidder2, BigDecimal.valueOf(15));

    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
    when(bidRepository.findActiveByAuctionId(auctionId)).thenReturn(List.of(bid1, bid2));
    when(walletRepository.findByUserId(bidder1)).thenReturn(Optional.of(wallet1));
    when(walletRepository.findByUserId(bidder2)).thenReturn(Optional.of(wallet2));

    // act
    CancelAuctionResult result = useCase.run(new CancelAuctionInput(auctionId, sellerId));

    // assert
    assertThat(result.releasedBids()).isEqualTo(2);
    verify(bidRepository, times(2)).save(any(Bid.class));
    verify(outboxEventRepository).save(any());
  }

  // --- no encontrada ---

  @Test
  void execute_auctionNotFound_throwsAuctionNotFoundException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(new CancelAuctionInput(auctionId, sellerId)))
        .isInstanceOf(AuctionNotFoundException.class);
  }

  // --- no autorizado ---

  @Test
  void execute_notOwner_throwsUnauthorizedAuctionAccessException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, sellerId, AuctionStatus.ACTIVE);
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

    // act & assert
    assertThatThrownBy(() -> useCase.run(new CancelAuctionInput(auctionId, otherUserId)))
        .isInstanceOf(UnauthorizedAuctionAccessException.class);
  }

  @Test
  void execute_notOwner_neverQueriesBids() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, sellerId, AuctionStatus.ACTIVE);
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

    // act
    assertThatThrownBy(() -> useCase.run(new CancelAuctionInput(auctionId, otherUserId)))
        .isInstanceOf(UnauthorizedAuctionAccessException.class);

    // assert
    verifyNoInteractions(bidRepository, walletRepository, outboxEventRepository);
  }

  // --- transicion invalida ---

  @Test
  void execute_alreadyAwardedAuction_throwsInvalidAuctionStatusTransitionException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, sellerId, AuctionStatus.AWARDED);
    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
    when(bidRepository.findActiveByAuctionId(auctionId)).thenReturn(List.of());

    // act & assert
    assertThatThrownBy(() -> useCase.run(new CancelAuctionInput(auctionId, sellerId)))
        .isInstanceOf(InvalidAuctionStatusTransitionException.class);
  }

  // --- wallet no encontrada ---

  @Test
  void execute_bidderWalletNotFound_throwsWalletNotFoundException() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    Auction auction = buildAuction(auctionId, sellerId, AuctionStatus.ACTIVE);
    Bid bid = buildActiveBid(auctionId, bidderId, BigDecimal.TEN);

    when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
    when(bidRepository.findActiveByAuctionId(auctionId)).thenReturn(List.of(bid));
    when(walletRepository.findByUserId(bidderId)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(new CancelAuctionInput(auctionId, sellerId)))
        .isInstanceOf(WalletNotFoundException.class);

    verify(auctionRepository, never()).save(any());
  }
}
