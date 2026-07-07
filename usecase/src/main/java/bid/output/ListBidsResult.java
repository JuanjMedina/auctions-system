package bid.output;

import domain.bid.BidStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListBidsResult(List<BidSummary> bids) {

  public record BidSummary(
      UUID bidId,
      UUID bidderId,
      BigDecimal amount,
      boolean autoBid,
      BidStatus status,
      Instant createdAt) {}
}
