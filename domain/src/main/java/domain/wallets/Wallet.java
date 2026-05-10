package domain.wallets;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = lombok.AccessLevel.PRIVATE)
public class Wallet {

  private final UUID id;
  private final UUID userId;
  private BigDecimal balance;
  private BigDecimal reservedBalance;
  private final String currency;
  private Long version;
  private Instant updatedAt;

  public static Wallet create(UUID userId) {
    return Wallet.builder()
        .id(UUID.randomUUID())
        .userId(userId)
        .balance(BigDecimal.ZERO)
        .reservedBalance(BigDecimal.ZERO)
        .currency("USD")
        .version(0L)
        .updatedAt(Instant.now())
        .build();
  }

  public static Wallet reconstitute(
      UUID id,
      UUID userId,
      BigDecimal balance,
      BigDecimal reservedBalance,
      String currency,
      Long version,
      Instant updatedAt) {
    return Wallet.builder()
        .id(id)
        .userId(userId)
        .balance(balance)
        .reservedBalance(reservedBalance)
        .currency(currency)
        .version(version)
        .updatedAt(updatedAt)
        .build();
  }

  public BigDecimal availableBalance() {
    return balance.subtract(reservedBalance);
  }

  public WalletTransaction deposit(BigDecimal amount, String description) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Deposit amount must be positive");
    }
    balance = balance.add(amount);
    updatedAt = Instant.now();
    return WalletTransaction.create(
        id, null, TransactionType.DEPOSIT, amount, balance, description);
  }

  public WalletTransaction withdraw(BigDecimal amount, String description) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Withdrawal amount must be positive");
    }
    if (availableBalance().compareTo(amount) < 0) {
      throw new IllegalStateException("Insufficient available balance");
    }
    balance = balance.subtract(amount);
    updatedAt = Instant.now();
    return WalletTransaction.create(
        id, null, TransactionType.WITHDRAWAL, amount, balance, description);
  }

  public WalletTransaction reserve(BigDecimal amount, UUID bidId) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Reserve amount must be positive");
    }
    if (availableBalance().compareTo(amount) < 0) {
      throw new IllegalStateException("Insufficient available balance to reserve");
    }
    reservedBalance = reservedBalance.add(amount);
    updatedAt = Instant.now();
    return WalletTransaction.create(
        id, bidId, TransactionType.BID_RESERVE, amount, balance, "Funds reserved for bid");
  }

  public WalletTransaction release(BigDecimal amount, UUID bidId) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Release amount must be positive");
    }
    if (reservedBalance.compareTo(amount) < 0) {
      throw new IllegalStateException("Cannot release more than reserved balance");
    }
    reservedBalance = reservedBalance.subtract(amount);
    updatedAt = Instant.now();
    return WalletTransaction.create(
        id, bidId, TransactionType.BID_RELEASE, amount, balance, "Released funds");
  }

  public WalletTransaction charge(BigDecimal amount, UUID auctionId) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Charge amount must be positive");
    }
    if (reservedBalance.compareTo(amount) < 0) {
      throw new IllegalStateException("Insufficient reserved balance to charge");
    }
    balance = balance.subtract(amount);
    reservedBalance = reservedBalance.subtract(amount);
    updatedAt = Instant.now();
    return WalletTransaction.create(
        id, auctionId, TransactionType.AUCTION_CHARGE, amount, balance, "Auction winning charge");
  }

  public WalletTransaction payout(BigDecimal amount, UUID auctionId) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Payout amount must be positive");
    }
    balance = balance.add(amount);
    updatedAt = Instant.now();
    return WalletTransaction.create(
        id, auctionId, TransactionType.AUCTION_PAYOUT, amount, balance, "Auction payout received");
  }
}
