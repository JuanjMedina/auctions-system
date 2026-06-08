package auction;

import auction.input.CancelAuctionInput;
import auction.output.CancelAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionExceptions;
import domain.auction.AuctionRepository;
import domain.bid.Bid;
import domain.bid.BidRepository;
import domain.outbox.AggregateType;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import domain.wallets.Wallet;
import domain.wallets.WalletExceptions;
import domain.wallets.WalletRepository;
import domain.wallets.WalletTransaction;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class CancelAuctionUseCase implements UseCase<CancelAuctionInput, CancelAuctionResult> {

  private final AuctionRepository auctionRepository;
  private final BidRepository bidRepository;
  private final WalletRepository walletRepository;
  private final OutboxEventRepository outboxEventRepository;

  @Override
  @Transactional
  public CancelAuctionResult execute(CancelAuctionInput input) {
    Auction auction =
        auctionRepository
            .findById(input.auctionId())
            .orElseThrow(() -> new AuctionExceptions.AuctionNotFoundException(input.auctionId()));

    if (!auction.isOwnedBy(input.sellerId())) {
      throw new AuctionExceptions.UnauthorizedAuctionAccessException(input.auctionId());
    }

    // Release reserves for all active bidders before cancelling
    List<Bid> activeBids = bidRepository.findActiveByAuctionId(input.auctionId());
    releaseAllReserves(activeBids);

    auction.cancel();
    auctionRepository.save(auction);

    outboxEventRepository.save(
        OutboxEvent.create(
            AggregateType.AUCTION,
            auction.getId(),
            EventType.AUCTION_CANCELLED,
            String.format(
                "{\"auctionId\":\"%s\",\"sellerId\":\"%s\",\"releasedBids\":%d}",
                auction.getId(), input.sellerId(), activeBids.size())));

    return new CancelAuctionResult(auction.getId(), auction.getStatus(), activeBids.size());
  }

  @Override
  public CancelAuctionResult failed(Exception exception) {
    if (exception instanceof AuctionExceptions.AuctionNotFoundException e) throw e;
    if (exception instanceof AuctionExceptions.UnauthorizedAuctionAccessException e) throw e;
    if (exception instanceof AuctionExceptions.InvalidAuctionStatusTransitionException e) throw e;
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al cancelar la subasta", exception);
  }

  private void releaseAllReserves(List<Bid> activeBids) {
    for (Bid bid : activeBids) {
      Wallet wallet =
          walletRepository
              .findByUserId(bid.getBidderId())
              .orElseThrow(() -> new WalletExceptions.WalletNotFoundException(bid.getBidderId()));

      WalletTransaction release = wallet.release(bid.getAmount(), bid.getId());
      bid.cancel();

      walletRepository.save(wallet);
      walletRepository.saveTransaction(release);
      bidRepository.save(bid);
    }
  }
}
