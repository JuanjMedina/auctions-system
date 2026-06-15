package entity.auction;

import domain.auction.AuctionStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "auctions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionJpaEntity implements Persistable<UUID> {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "seller_id", nullable = false)
  private UUID sellerId;

  @Column(name = "category_id", nullable = false)
  private UUID categoryId;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "starting_price", nullable = false, precision = 19, scale = 4)
  private BigDecimal startingPrice;

  @Column(name = "reserve_price", precision = 19, scale = 4)
  private BigDecimal reservePrice;

  @Column(name = "current_price", nullable = false, precision = 19, scale = 4)
  private BigDecimal currentPrice;

  @Column(name = "current_winner_id")
  private UUID currentWinnerId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private AuctionStatus status;

  @Column(name = "auto_extend", nullable = false)
  private boolean autoExtend;

  @Column(name = "extend_minutes", nullable = false)
  private int extendMinutes;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  private Instant endsAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @OneToMany(
      mappedBy = "auction",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @Builder.Default
  private List<AuctionImageJpaEntity> images = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @Override
  public boolean isNew() {
    return version == null;
  }
}
