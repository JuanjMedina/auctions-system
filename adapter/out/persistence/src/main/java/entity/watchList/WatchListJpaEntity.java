package entity.watchList;

import entity.auction.AuctionJpaEntity;
import entity.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "watchlist",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "auction_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchListJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "user_id", insertable = false, updatable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "auction_id", nullable = false)
  private AuctionJpaEntity auction;

  @Column(name = "auction_id", insertable = false, updatable = false)
  private UUID auctionId;

  @CreationTimestamp
  @Column(name = "added_at", nullable = false, updatable = false)
  private Instant addedAt;
}
