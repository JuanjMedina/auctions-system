package bid;

import auction.GetAuctionUseCase;
import auction.input.GetAuctionInput;
import auction.output.GetAuctionResult;
import bid.input.PlaceBidInput;
import bid.output.PlaceBidOutput;
import domain.auction.AuctionExceptions.AuctionAlreadyClosedException;
import domain.auction.AuctionRepository;
import domain.bid.BidRepository;
import domain.outbox.OutboxEventRepository;
import domain.wallets.WalletRepository;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;
import wallet.GetWalletUseCase;

@Service
@AllArgsConstructor
public class PlaceBidUseCase implements UseCase<PlaceBidInput, PlaceBidOutput> {

  private final BidRepository bidRepository;
  private final AuctionRepository auctionRepository;
  private final WalletRepository walletRepository;
  private final OutboxEventRepository outboxEventRepository;
  private final GetAuctionUseCase getAuctionUseCase;
  private final GetWalletUseCase getWalletUseCase;

  @Override
  public PlaceBidOutput execute(PlaceBidInput input) {

    GetAuctionResult auction = getAuctionUseCase.run(new GetAuctionInput(input.auctionId()));

    Instant now = Instant.now();
    if (auction.endsAt().isBefore(now)) {
      throw new AuctionAlreadyClosedException(auction.id());
    }

    return null;
  }

  @Override
  public PlaceBidOutput failed(Exception exception) {
    return null;
  }
}
