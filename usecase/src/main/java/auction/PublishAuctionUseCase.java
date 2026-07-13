package auction;

import auction.input.PublishAuctionInput;
import auction.output.PublishAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionExceptions;
import domain.auction.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class PublishAuctionUseCase implements UseCase<PublishAuctionInput, PublishAuctionResult> {

  private final AuctionRepository auctionRepository;

  @Override
  @Transactional
  public PublishAuctionResult execute(PublishAuctionInput input) {
    Auction auction = auctionRepository.getById(input.auctionId());

    if (!auction.isOwnedBy(input.sellerId())) {
      throw new AuctionExceptions.UnauthorizedAuctionAccessException(input.auctionId());
    }

    auction.publish();
    Auction saved = auctionRepository.save(auction);
    return new PublishAuctionResult(saved.getId(), saved.getStatus());
  }

  @Override
  public String errorMessage() {
    return "Error al publicar la subasta";
  }
}
