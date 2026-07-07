package watchList.output;

import auction.output.AuctionSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListWatchListResult(List<WatchListEntry> entries) {

  public record WatchListEntry(UUID watchListId, Instant addedAt, AuctionSummary auction) {}
}
