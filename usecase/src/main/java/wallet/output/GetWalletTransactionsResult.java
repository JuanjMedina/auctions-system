package wallet.output;

import domain.wallets.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GetWalletTransactionsResult(List<WalletTransactionSummary> transactions) {

  public record WalletTransactionSummary(
      UUID id,
      UUID referenceId,
      TransactionType type,
      BigDecimal amount,
      BigDecimal balanceAfter,
      String description,
      Instant createdAt) {}
}
