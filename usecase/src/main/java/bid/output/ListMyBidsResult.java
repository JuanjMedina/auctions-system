package bid.output;

import domain.bid.BidStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListMyBidsResult(List<MyBidSummary> bids) {

  public record MyBidSummary(
      UUID bidId,
      UUID auctionId,
      BigDecimal amount,
      boolean autoBid,
      BidStatus status,
      Instant createdAt) {}
}
