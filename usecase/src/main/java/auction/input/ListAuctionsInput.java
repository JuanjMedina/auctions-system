package auction.input;

import domain.auction.AuctionStatus;
import java.util.Optional;
import java.util.UUID;

public record ListAuctionsInput(
    Optional<AuctionStatus> status, Optional<UUID> categoryId, Integer page, Integer size) {}
