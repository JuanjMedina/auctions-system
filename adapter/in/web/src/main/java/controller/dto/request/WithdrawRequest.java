package controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record WithdrawRequest(@NotNull @Positive BigDecimal amount, String description) {}
