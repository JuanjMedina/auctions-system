package auction.output;

import domain.shared.PageResult;

public record ListAuctionsResult(PageResult<AuctionSummary> page) {}
