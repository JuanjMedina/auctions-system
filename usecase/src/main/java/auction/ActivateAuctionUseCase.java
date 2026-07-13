package auction;

import auction.input.ActivateAuctionInput;
import auction.output.ActivateAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class ActivateAuctionUseCase
    implements UseCase<ActivateAuctionInput, ActivateAuctionResult> {

  private final AuctionRepository auctionRepository;

  @Override
  @Transactional
  public ActivateAuctionResult execute(ActivateAuctionInput input) {
    Auction auction = auctionRepository.getById(input.auctionId());

    auction.activate();
    Auction saved = auctionRepository.save(auction);
    return new ActivateAuctionResult(saved.getId(), saved.getStatus());
  }

  @Override
  public String errorMessage() {
    return "Error al activar la subasta";
  }
}
