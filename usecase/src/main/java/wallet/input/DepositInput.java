package wallet.input;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositInput(UUID userId, BigDecimal amount, String description) {}
