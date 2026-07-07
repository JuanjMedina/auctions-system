package auction;

import auction.input.ListMyAuctionsInput;
import auction.output.AuctionSummary;
import auction.output.ListMyAuctionsResult;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class ListMyAuctionsUseCase implements UseCase<ListMyAuctionsInput, ListMyAuctionsResult> {

  private final AuctionRepository auctionRepository;

  @Override
  public ListMyAuctionsResult execute(ListMyAuctionsInput input) {
    List<Auction> auctions = auctionRepository.findBySellerId(input.sellerId());

    List<AuctionSummary> summaries =
        auctions.stream().map(ListMyAuctionsUseCase::toSummary).toList();

    return new ListMyAuctionsResult(summaries);
  }

  @Override
  public ListMyAuctionsResult failed(Exception exception) {
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al listar mis subastas", exception);
  }

  private static AuctionSummary toSummary(Auction auction) {
    return AuctionSummary.builder()
        .id(auction.getId())
        .title(auction.getTitle())
        .description(auction.getDescription())
        .categoryId(auction.getCategoryId())
        .sellerId(auction.getSellerId())
        .currentPrice(auction.getCurrentPrice())
        .status(auction.getStatus())
        .startsAt(auction.getStartsAt())
        .endsAt(auction.getEndsAt())
        .build();
  }
}
