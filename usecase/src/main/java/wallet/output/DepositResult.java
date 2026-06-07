package wallet.output;

import domain.wallets.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DepositResult(
    UUID transactionId,
    BigDecimal amount,
    BigDecimal balanceAfter,
    String currency,
    TransactionType type,
    Instant createdAt) {}
