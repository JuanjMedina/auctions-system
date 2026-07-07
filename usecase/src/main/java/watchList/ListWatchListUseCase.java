package watchList;

import auction.output.AuctionSummary;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import domain.watchList.WatchList;
import domain.watchList.WatchListRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;
import watchList.input.ListWatchListInput;
import watchList.output.ListWatchListResult;
import watchList.output.ListWatchListResult.WatchListEntry;

@Service
@RequiredArgsConstructor
public class ListWatchListUseCase implements UseCase<ListWatchListInput, ListWatchListResult> {

  private final WatchListRepository watchListRepository;
  private final AuctionRepository auctionRepository;

  @Override
  public ListWatchListResult execute(ListWatchListInput input) {
    List<WatchList> watched = watchListRepository.findByUserId(input.userId());

    List<WatchListEntry> entries =
        watched.stream().map(this::toEntry).filter(Optional::isPresent).map(Optional::get).toList();

    return new ListWatchListResult(entries);
  }

  @Override
  public ListWatchListResult failed(Exception exception) {
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al listar los favoritos", exception);
  }

  private Optional<WatchListEntry> toEntry(WatchList watchList) {
    return auctionRepository
        .findById(watchList.getAuctionId())
        .map(
            auction ->
                new WatchListEntry(
                    watchList.getId(), watchList.getAddedAt(), toAuctionSummary(auction)));
  }

  private static AuctionSummary toAuctionSummary(Auction auction) {
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
