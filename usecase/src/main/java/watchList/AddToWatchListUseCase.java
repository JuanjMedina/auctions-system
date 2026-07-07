package watchList;

import domain.auction.AuctionExceptions;
import domain.auction.AuctionRepository;
import domain.watchList.WatchList;
import domain.watchList.WatchListExceptions;
import domain.watchList.WatchListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;
import watchList.input.AddToWatchListInput;
import watchList.output.AddToWatchListResult;

@Service
@RequiredArgsConstructor
public class AddToWatchListUseCase implements UseCase<AddToWatchListInput, AddToWatchListResult> {

  private final AuctionRepository auctionRepository;
  private final WatchListRepository watchListRepository;

  @Override
  public AddToWatchListResult execute(AddToWatchListInput input) {
    auctionRepository
        .findById(input.auctionId())
        .orElseThrow(() -> new AuctionExceptions.AuctionNotFoundException(input.auctionId()));

    if (watchListRepository.existsByUserIdAndAuctionId(input.userId(), input.auctionId())) {
      throw new WatchListExceptions.AlreadyInWatchListException(input.userId(), input.auctionId());
    }

    WatchList saved = watchListRepository.save(WatchList.create(input.userId(), input.auctionId()));

    return new AddToWatchListResult(saved.getId(), saved.getAuctionId(), saved.getAddedAt());
  }

  @Override
  public AddToWatchListResult failed(Exception exception) {
    if (exception instanceof AuctionExceptions.AuctionNotFoundException e) throw e;
    if (exception instanceof WatchListExceptions.AlreadyInWatchListException e) throw e;
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al agregar la subasta a favoritos", exception);
  }
}
