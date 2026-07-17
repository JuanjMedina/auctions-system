package auction;

import auction.input.ActivateAuctionInput;
import auction.output.ActivateAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import domain.outbox.AggregateType;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class ActivateAuctionUseCase
    implements UseCase<ActivateAuctionInput, ActivateAuctionResult> {

  private final AuctionRepository auctionRepository;
  private final OutboxEventRepository outboxEventRepository;

  @Override
  @Transactional
  public ActivateAuctionResult run(ActivateAuctionInput input) {
    return UseCase.super.run(input);
  }

  @Override
  public ActivateAuctionResult execute(ActivateAuctionInput input) {
    Auction auction = auctionRepository.getById(input.auctionId());

    auction.activate();
    Auction saved = auctionRepository.save(auction);

    outboxEventRepository.save(
        OutboxEvent.create(
            AggregateType.AUCTION,
            saved.getId(),
            EventType.AUCTION_ACTIVATED,
            String.format(
                "{\"auctionId\":\"%s\",\"endsAt\":\"%s\"}", saved.getId(), saved.getEndsAt())));

    return new ActivateAuctionResult(saved.getId(), saved.getStatus());
  }

  @Override
  public String errorMessage() {
    return "Error al activar la subasta";
  }
}
