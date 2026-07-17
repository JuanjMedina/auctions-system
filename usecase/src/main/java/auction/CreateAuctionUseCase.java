package auction;

import auction.input.CreateAuctionInput;
import auction.output.CreateAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class CreateAuctionUseCase implements UseCase<CreateAuctionInput, CreateAuctionResult> {

  private final AuctionRepository auctionRepository;
  private final OutboxEventRepository outboxEventRepository;

  @Override
  @Transactional
  public CreateAuctionResult run(CreateAuctionInput input) {
    return UseCase.super.run(input);
  }

  @Override
  public CreateAuctionResult execute(CreateAuctionInput input) {
    Auction auction =
        Auction.create(
            input.sellerId(),
            input.categoryId(),
            input.title(),
            input.description(),
            input.startingPrice(),
            input.reservePrice(),
            input.startsAt(),
            input.endsAt(),
            input.autoExtend(),
            input.extendMinutes());

    Auction saved = auctionRepository.save(auction);

    return new CreateAuctionResult(saved.getId(), saved.getStatus(), saved.getCreatedAt());
  }

  @Override
  public String errorMessage() {
    return "Error al crear la subasta";
  }
}
