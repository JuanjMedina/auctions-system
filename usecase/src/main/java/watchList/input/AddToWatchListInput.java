package watchList.input;

import java.util.UUID;

public record AddToWatchListInput(UUID userId, UUID auctionId) {}
