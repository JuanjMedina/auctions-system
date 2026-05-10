package domain.wallets;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
public class WalletTransaction {

  private final UUID id;
  private final UUID walletId;
  private final UUID referenceId; // nullable — bid_id o auction_id que originó el movimiento
  private final TransactionType type;
  private final BigDecimal amount;
  private final BigDecimal balanceAfter;
  private final String description;
  private final Instant createdAt;

  private WalletTransaction(
      UUID id,
      UUID walletId,
      UUID referenceId,
      TransactionType type,
      BigDecimal amount,
      BigDecimal balanceAfter,
      String description,
      Instant createdAt) {
    this.id = id;
    this.walletId = walletId;
    this.referenceId = referenceId;
    this.type = type;
    this.amount = amount;
    this.balanceAfter = balanceAfter;
    this.description = description;
    this.createdAt = createdAt;
  }

  public static WalletTransaction create(
      UUID walletId,
      UUID referenceId,
      TransactionType type,
      BigDecimal amount,
      BigDecimal balanceAfter,
      String description) {
    return new WalletTransaction(
        UUID.randomUUID(),
        walletId,
        referenceId,
        type,
        amount,
        balanceAfter,
        description,
        Instant.now());
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
    return new WalletTransaction(
        id, walletId, referenceId, type, amount, balanceAfter, description, createdAt);
  }
}
