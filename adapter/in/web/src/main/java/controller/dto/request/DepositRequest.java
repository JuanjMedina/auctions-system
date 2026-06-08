package controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record DepositRequest(@NotNull @Positive BigDecimal amount, String description) {}
