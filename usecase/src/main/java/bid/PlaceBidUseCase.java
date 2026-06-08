package bid;

import bid.input.PlaceBidInput;
import bid.output.PlaceBidOutput;
import domain.auction.Auction;
import domain.auction.AuctionExceptions;
import domain.auction.AuctionRepository;
import domain.bid.Bid;
import domain.bid.BidRepository;
import domain.wallets.Wallet;
import domain.wallets.WalletExceptions;
import domain.wallets.WalletRepository;
import domain.wallets.WalletTransaction;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class PlaceBidUseCase implements UseCase<PlaceBidInput, PlaceBidOutput> {

  private final AuctionRepository auctionRepository;
  private final BidRepository bidRepository;
  private final WalletRepository walletRepository;

  @Override
  @Transactional
  public PlaceBidOutput execute(PlaceBidInput input) {
    Auction auction =
        auctionRepository
            .findById(input.auctionId())
            .orElseThrow(() -> new AuctionExceptions.AuctionNotFoundException(input.auctionId()));

    Wallet bidderWallet =
        walletRepository
            .findByUserId(input.bidderId())
            .orElseThrow(() -> new WalletExceptions.WalletNotFoundException(input.bidderId()));

    if (bidderWallet.availableBalance().compareTo(input.amount()) < 0) {
      throw new WalletExceptions.InsufficientFundsException(
          bidderWallet.availableBalance(), input.amount());
    }

    // Capture the previous winner before auction updates its state
    UUID previousWinnerId = auction.getCurrentWinnerId();

    // Validates: auction ACTIVE, bidder != seller, amount > currentPrice
    auction.placeBid(input.bidderId(), input.amount());

    // Release funds of the bidder just outbid
    if (previousWinnerId != null) {
      releaseOutbidFunds(input.auctionId(), previousWinnerId);
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

    return new PlaceBidOutput(
        saved.getId(),
        saved.getAuctionId(),
        saved.getAmount(),
        saved.getStatus(),
        saved.getCreatedAt());
  }

  @Override
  public PlaceBidOutput failed(Exception exception) {
    if (exception instanceof AuctionExceptions.AuctionNotFoundException e) throw e;
    if (exception instanceof AuctionExceptions.AuctionNotActiveException e) throw e;
    if (exception instanceof AuctionExceptions.BidTooLowException e) throw e;
    if (exception instanceof AuctionExceptions.SellerCannotBidException e) throw e;
    if (exception instanceof WalletExceptions.WalletNotFoundException e) throw e;
    if (exception instanceof WalletExceptions.InsufficientFundsException e) throw e;
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al realizar la puja", exception);
  }

  private void releaseOutbidFunds(UUID auctionId, UUID previousWinnerId) {
    bidRepository
        .findLatestActiveBidByAuctionIdAndBidderId(auctionId, previousWinnerId)
        .ifPresent(
            previous -> {
              Wallet previousWallet =
                  walletRepository
                      .findByUserId(previousWinnerId)
                      .orElseThrow(
                          () -> new WalletExceptions.WalletNotFoundException(previousWinnerId));
              WalletTransaction release =
                  previousWallet.release(previous.getAmount(), previous.getId());
              previous.markAsOutbid();
              walletRepository.save(previousWallet);
              walletRepository.saveTransaction(release);
              bidRepository.save(previous);
            });
  }
}
