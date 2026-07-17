package bid;

import bid.input.PlaceBidInput;
import bid.output.PlaceBidOutput;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import domain.bid.Bid;
import domain.bid.BidRepository;
import domain.outbox.AggregateType;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import domain.shared.ConcurrencyException;
import domain.wallets.Wallet;
import domain.wallets.WalletExceptions;
import domain.wallets.WalletRepository;
import domain.wallets.WalletTransaction;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class PlaceBidUseCase implements UseCase<PlaceBidInput, PlaceBidOutput> {

  private final AuctionRepository auctionRepository;
  private final BidRepository bidRepository;
  private final WalletRepository walletRepository;
  private final OutboxEventRepository outboxEventRepository;

  @Override
  @Transactional
  @Retryable(
      retryFor = ConcurrencyException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 100, multiplier = 2))
  public PlaceBidOutput run(PlaceBidInput input) {
    return UseCase.super.run(input);
  }

  @Override
  public PlaceBidOutput execute(PlaceBidInput input) {
    Auction auction = auctionRepository.getById(input.auctionId());

    Wallet bidderWallet = walletRepository.getByUserId(input.bidderId());

    if (bidderWallet.availableBalance().compareTo(input.amount()) < 0) {
      throw new WalletExceptions.InsufficientFundsException(
          bidderWallet.availableBalance(), input.amount());
    }

    // Capture the previous winner before auction updates its state
    UUID previousWinnerId = auction.getCurrentWinnerId();

    // Validates: auction ACTIVE/EXTENDED, bidder != seller, amount > currentPrice
    auction.placeBid(input.bidderId(), input.amount());

    // Release funds of the bidder just outbid
    if (previousWinnerId != null) {
      releaseOutbidFunds(input.auctionId(), previousWinnerId, input.amount());
    }

    // Create bid and reserve funds atomically
    Bid newBid =
        Bid.create(
            input.auctionId(),
            input.bidderId(),
            input.amount(),
            input.autoBid(),
            input.maxAutoAmount());

    WalletTransaction reserve = bidderWallet.reserve(input.amount(), newBid.getId());

    walletRepository.save(bidderWallet);
    walletRepository.saveTransaction(reserve);
    auctionRepository.save(auction);
    Bid saved = bidRepository.save(newBid);

    // Publish BID_PLACED event via outbox pattern
    outboxEventRepository.save(
        OutboxEvent.create(
            AggregateType.BID,
            saved.getId(),
            EventType.BID_PLACED,
            buildPayload(saved, auction.getCurrentPrice().toString())));

    return new PlaceBidOutput(
        saved.getId(),
        saved.getAuctionId(),
        saved.getAmount(),
        saved.getStatus(),
        saved.getCreatedAt());
  }

  @Override
  public String errorMessage() {
    return "Error al realizar la puja";
  }

  private void releaseOutbidFunds(UUID auctionId, UUID previousWinnerId, BigDecimal newAmount) {
    bidRepository
        .findLatestActiveBidByAuctionIdAndBidderId(auctionId, previousWinnerId)
        .ifPresent(
            previous -> {
              Wallet previousWallet = walletRepository.getByUserId(previousWinnerId);
              WalletTransaction release =
                  previousWallet.release(previous.getAmount(), previous.getId());
              previous.markAsOutbid();
              walletRepository.save(previousWallet);
              walletRepository.saveTransaction(release);
              bidRepository.save(previous);

              outboxEventRepository.save(
                  OutboxEvent.create(
                      AggregateType.BID,
                      previous.getId(),
                      EventType.BID_OUTBID,
                      String.format(
                          "{\"bidId\":\"%s\",\"auctionId\":\"%s\",\"outbidUserId\":\"%s\",\"newAmount\":\"%s\"}",
                          previous.getId(), auctionId, previousWinnerId, newAmount)));
            });
  }

  private String buildPayload(Bid bid, String currentPrice) {
    return String.format(
        "{\"bidId\":\"%s\",\"auctionId\":\"%s\",\"bidderId\":\"%s\",\"amount\":\"%s\",\"currentPrice\":\"%s\"}",
        bid.getId(), bid.getAuctionId(), bid.getBidderId(), bid.getAmount(), currentPrice);
  }
}
