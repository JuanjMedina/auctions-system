package bid;

import bid.input.DeleteBidInput;
import bid.output.DeleteBidOutput;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import domain.bid.Bid;
import domain.bid.BidExceptions;
import domain.bid.BidRepository;
import domain.bid.BidStatus;
import domain.outbox.AggregateType;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import domain.wallets.Wallet;
import domain.wallets.WalletRepository;
import domain.wallets.WalletTransaction;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class DeleteBidUseCase implements UseCase<DeleteBidInput, DeleteBidOutput> {

  private final BidRepository bidRepository;
  private final AuctionRepository auctionRepository;
  private final WalletRepository walletRepository;
  private final OutboxEventRepository outboxEventRepository;

  @Override
  @Transactional
  public DeleteBidOutput run(DeleteBidInput input) {
    return UseCase.super.run(input);
  }

  @Override
  public DeleteBidOutput execute(DeleteBidInput input) {
    Bid bid = bidRepository.getById(input.bidId());

    if (!bid.getAuctionId().equals(input.auctionId())) {
      throw new BidExceptions.BidNotFoundException(input.bidId());
    }

    if (!bid.getBidderId().equals(input.bidderId())) {
      throw new BidExceptions.UnauthorizedBidAccessException(input.bidId());
    }

    if (bid.getStatus() != BidStatus.ACTIVE) {
      throw new BidExceptions.InvalidBidStatusTransitionException(
          bid.getId(), bid.getStatus(), BidStatus.CANCELLED);
    }

    Auction auction = auctionRepository.getById(input.auctionId());

    bid.cancel();
    releaseReservedFunds(bid);

    if (wasAuctionLeader(bid)) {
      promoteNextBestBid(bid, auction);
    }

    bidRepository.save(bid);

    outboxEventRepository.save(
        OutboxEvent.create(
            AggregateType.BID,
            bid.getId(),
            EventType.BID_CANCELLED,
            String.format(
                "{\"bidId\":\"%s\",\"auctionId\":\"%s\",\"bidderId\":\"%s\",\"amount\":\"%s\"}",
                bid.getId(), bid.getAuctionId(), bid.getBidderId(), bid.getAmount())));

    return new DeleteBidOutput(bid.getId(), bid.getAuctionId(), bid.getStatus());
  }

  @Override
  public String errorMessage() {
    return "Error al cancelar la puja";
  }

  // La puja aun figura ACTIVE en base de datos: bid.cancel() solo mutó el objeto en memoria,
  // el guardado ocurre al final de execute().
  private boolean wasAuctionLeader(Bid bid) {
    return bidRepository.findActiveByAuctionId(bid.getAuctionId()).stream()
        .max((b1, b2) -> b1.getAmount().compareTo(b2.getAmount()))
        .map(Bid::getId)
        .filter(leaderId -> leaderId.equals(bid.getId()))
        .isPresent();
  }

  private void releaseReservedFunds(Bid bid) {
    Wallet bidderWallet = walletRepository.getByUserId(bid.getBidderId());
    WalletTransaction release = bidderWallet.release(bid.getAmount(), bid.getId());
    walletRepository.save(bidderWallet);
    walletRepository.saveTransaction(release);
  }

  private void promoteNextBestBid(Bid cancelledBid, Auction auction) {
    Optional<Bid> nextBestBid =
        bidRepository.findByAuctionIdOrderByAmountDesc(cancelledBid.getAuctionId()).stream()
            .filter(b -> b.getStatus() == BidStatus.OUTBID)
            .findFirst();

    if (nextBestBid.isPresent()) {
      Bid promotedBid = nextBestBid.get();
      promotedBid.reactivate();
      bidRepository.save(promotedBid);
      auction.revertToPreviousBid(promotedBid.getBidderId(), promotedBid.getAmount());
    } else {
      auction.resetToNoWinner();
    }
    auctionRepository.save(auction);
  }
}
