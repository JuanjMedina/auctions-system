package entity;

import domain.bid.BidStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bids")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidJpaEntity {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "auction_id", nullable = false)
  private UUID auctionId;

  @Column(name = "bidder_id", nullable = false)
  private UUID bidderId;

  @Column(name = "amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  @Column(name = "is_auto_bid", nullable = false)
  private boolean isAutoBid;

  @Column(name = "max_auto_amount", precision = 19, scale = 4)
  private BigDecimal maxAutoAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private BidStatus status;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;
}
