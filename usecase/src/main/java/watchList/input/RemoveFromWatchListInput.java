package watchList.input;

import java.util.UUID;

public record RemoveFromWatchListInput(UUID userId, UUID auctionId) {}
