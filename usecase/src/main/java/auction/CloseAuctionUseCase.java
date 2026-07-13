package auction;

import auction.input.CloseAuctionInput;
import auction.output.CloseAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import domain.bid.Bid;
import domain.bid.BidRepository;
import domain.outbox.AggregateType;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import domain.wallets.Wallet;
import domain.wallets.WalletRepository;
import domain.wallets.WalletTransaction;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class CloseAuctionUseCase implements UseCase<CloseAuctionInput, CloseAuctionResult> {

  private final AuctionRepository auctionRepository;
  private final BidRepository bidRepository;
  private final WalletRepository walletRepository;
  private final OutboxEventRepository outboxEventRepository;

  @Override
  @Transactional
  public CloseAuctionResult execute(CloseAuctionInput input) {
    Auction auction = auctionRepository.getById(input.auctionId());

    // Domain decides AWARDED or FAILED based on winner + reservePrice
    auction.close();

    List<Bid> activeBids = bidRepository.findActiveByAuctionId(input.auctionId());

    if (auction.getStatus() == AuctionStatus.AWARDED) {
      handleAwarded(auction, activeBids);
    } else {
      handleFailed(auction, activeBids);
    }

    auctionRepository.save(auction);

    return new CloseAuctionResult(
        auction.getId(), auction.getStatus(), auction.getCurrentWinnerId());
  }

  @Override
  public String errorMessage() {
    return "Error al cerrar la subasta";
  }

  // -------------------------------------------------------------------------
  // AWARDED: winner pays, seller receives, other active bids (edge) released
  // -------------------------------------------------------------------------

  private void handleAwarded(Auction auction, List<Bid> activeBids) {
    UUID winnerId = auction.getCurrentWinnerId();

    Wallet winnerWallet = loadWallet(winnerId);
    Wallet sellerWallet = loadWallet(auction.getSellerId());

    // charge deducts from balance AND reservedBalance (consumes the existing reserve)
    WalletTransaction charge = winnerWallet.charge(auction.getCurrentPrice(), auction.getId());
    // payout credits the seller
    WalletTransaction payout = sellerWallet.payout(auction.getCurrentPrice(), auction.getId());

    walletRepository.save(winnerWallet);
    walletRepository.saveTransaction(charge);
    walletRepository.save(sellerWallet);
    walletRepository.saveTransaction(payout);

    for (Bid bid : activeBids) {
      if (bid.getBidderId().equals(winnerId)) {
        bid.markAsWinning();
      } else {
        // edge case: active bid from another bidder still present
        releaseAndCancel(bid);
      }
      bidRepository.save(bid);
    }

    outboxEventRepository.save(
        OutboxEvent.create(
            AggregateType.AUCTION,
            auction.getId(),
            EventType.AUCTION_AWARDED,
            String.format(
                "{\"auctionId\":\"%s\",\"winnerId\":\"%s\",\"finalPrice\":\"%s\"}",
                auction.getId(), winnerId, auction.getCurrentPrice())));
  }

  // -------------------------------------------------------------------------
  // FAILED: no winner or reserve not met — release all active reserves
  // -------------------------------------------------------------------------

  private void handleFailed(Auction auction, List<Bid> activeBids) {
    for (Bid bid : activeBids) {
      releaseAndCancel(bid);
      bidRepository.save(bid);
    }

    outboxEventRepository.save(
        OutboxEvent.create(
            AggregateType.AUCTION,
            auction.getId(),
            EventType.AUCTION_FAILED,
            String.format(
                "{\"auctionId\":\"%s\",\"sellerId\":\"%s\"}",
                auction.getId(), auction.getSellerId())));
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private void releaseAndCancel(Bid bid) {
    Wallet wallet = loadWallet(bid.getBidderId());
    WalletTransaction release = wallet.release(bid.getAmount(), bid.getId());
    bid.cancel();
    walletRepository.save(wallet);
    walletRepository.saveTransaction(release);
  }

  private Wallet loadWallet(UUID userId) {
    return walletRepository.getByUserId(userId);
  }
}
