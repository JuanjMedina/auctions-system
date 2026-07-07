package auction.output;

import java.util.List;

public record ListMyAuctionsResult(List<AuctionSummary> auctions) {}
