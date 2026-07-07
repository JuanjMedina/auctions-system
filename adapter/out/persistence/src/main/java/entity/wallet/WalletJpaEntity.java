package entity.wallet;

import entity.user.UserEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletJpaEntity {

  @Id private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private UserEntity user;

  @Column(name = "user_id", insertable = false, updatable = false)
  private UUID userId;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal balance;

  @Column(name = "reserved_balance", nullable = false, precision = 19, scale = 4)
  private BigDecimal reservedBalance;

  @Column(nullable = false, length = 3)
  private String currency;

  @Version
  @Column(nullable = false)
  private Long version;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "wallet", fetch = FetchType.LAZY)
  @Builder.Default
  private List<WalletTransactionJpaEntity> transactions = new ArrayList<>();
}
