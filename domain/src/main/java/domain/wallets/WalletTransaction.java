package domain.wallets;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = lombok.AccessLevel.PRIVATE)
public class WalletTransaction {

  private final UUID id;
  private final UUID walletId;
  private final UUID referenceId;
  private final TransactionType type;
  private final BigDecimal amount;
  private final BigDecimal balanceAfter;
  private final String description;
  private final Instant createdAt;

  public static WalletTransaction create(
      UUID walletId,
      UUID referenceId,
      TransactionType type,
      BigDecimal amount,
      BigDecimal balanceAfter,
      String description) {
    return WalletTransaction.builder()
        .id(UUID.randomUUID())
        .walletId(walletId)
        .referenceId(referenceId)
        .type(type)
        .amount(amount)
        .balanceAfter(balanceAfter)
        .description(description)
        .createdAt(Instant.now())
        .build();
  }

  public static WalletTransaction reconstitute(
      UUID id,
      UUID walletId,
      UUID referenceId,
      TransactionType type,
      BigDecimal amount,
      BigDecimal balanceAfter,
      String description,
      Instant createdAt) {
    return WalletTransaction.builder()
        .id(id)
        .walletId(walletId)
        .referenceId(referenceId)
        .type(type)
        .amount(amount)
        .balanceAfter(balanceAfter)
        .description(description)
        .createdAt(createdAt)
        .build();
  }
}
