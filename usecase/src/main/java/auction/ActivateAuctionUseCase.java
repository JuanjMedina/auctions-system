package auction;

import auction.input.ActivateAuctionInput;
import auction.output.ActivateAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionExceptions;
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
    Auction auction =
        auctionRepository
            .findById(input.auctionId())
            .orElseThrow(() -> new AuctionExceptions.AuctionNotFoundException(input.auctionId()));

    auction.activate();
    Auction saved = auctionRepository.save(auction);
    return new ActivateAuctionResult(saved.getId(), saved.getStatus());
  }

  @Override
  public ActivateAuctionResult failed(Exception exception) {
    if (exception instanceof AuctionExceptions.AuctionNotFoundException e) throw e;
    if (exception instanceof AuctionExceptions.InvalidAuctionStatusTransitionException e) throw e;
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al activar la subasta", exception);
  }
}
