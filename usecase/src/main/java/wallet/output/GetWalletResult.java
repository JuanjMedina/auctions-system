package wallet.output;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GetWalletResult(
    UUID id,
    UUID userId,
    BigDecimal balance,
    BigDecimal reservedBalance,
    BigDecimal availableBalance,
    String currency,
    Instant updatedAt) {}
