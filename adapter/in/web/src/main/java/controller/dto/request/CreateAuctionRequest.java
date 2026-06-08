package controller.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateAuctionRequest(
    @NotNull UUID categoryId,
    @NotBlank String title,
    String description,
    @NotNull @Positive BigDecimal startingPrice,
    BigDecimal reservePrice,
    @NotNull @Future Instant startsAt,
    @NotNull @Future Instant endsAt,
    boolean autoExtend,
    int extendMinutes) {}
