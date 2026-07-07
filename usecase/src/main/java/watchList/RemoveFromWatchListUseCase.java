package watchList;

import domain.watchList.WatchList;
import domain.watchList.WatchListExceptions;
import domain.watchList.WatchListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;
import watchList.input.RemoveFromWatchListInput;
import watchList.output.RemoveFromWatchListResult;

@Service
@RequiredArgsConstructor
public class RemoveFromWatchListUseCase
    implements UseCase<RemoveFromWatchListInput, RemoveFromWatchListResult> {

  private final WatchListRepository watchListRepository;

  @Override
  public RemoveFromWatchListResult execute(RemoveFromWatchListInput input) {
    WatchList entry =
        watchListRepository
            .findByUserIdAndAuctionId(input.userId(), input.auctionId())
            .orElseThrow(
                () ->
                    new WatchListExceptions.WatchListEntryNotFoundException(
                        input.userId(), input.auctionId()));

    watchListRepository.delete(entry);

    return new RemoveFromWatchListResult(input.auctionId());
  }

  @Override
  public RemoveFromWatchListResult failed(Exception exception) {
    if (exception instanceof WatchListExceptions.WatchListEntryNotFoundException e) throw e;
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al quitar la subasta de favoritos", exception);
  }
}
