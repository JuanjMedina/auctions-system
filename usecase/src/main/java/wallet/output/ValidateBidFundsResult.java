package wallet.output;

import java.math.BigDecimal;

public record ValidateBidFundsResult(
    boolean valid, BigDecimal availableBalance, BigDecimal required) {}
