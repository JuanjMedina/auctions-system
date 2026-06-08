package wallet.input;

import java.math.BigDecimal;
import java.util.UUID;

public record ValidateBidFundsInput(UUID userId, BigDecimal bidAmount) {}
