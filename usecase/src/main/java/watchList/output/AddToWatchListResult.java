package watchList.output;

import java.time.Instant;
import java.util.UUID;

public record AddToWatchListResult(UUID id, UUID auctionId, Instant addedAt) {}
