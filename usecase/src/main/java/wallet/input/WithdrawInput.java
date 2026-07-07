package wallet.input;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawInput(UUID userId, BigDecimal amount, String description) {}
