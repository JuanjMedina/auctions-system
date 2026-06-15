package domain.auction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = lombok.AccessLevel.PRIVATE)
public class Auction {

  private final UUID id;
  private final UUID sellerId;
  private UUID categoryId;
  private String title;
  private String description;
  private final BigDecimal startingPrice;
  private BigDecimal reservePrice;
  private BigDecimal currentPrice;
  private UUID currentWinnerId;
  private AuctionStatus status;
  private final Instant startsAt;
  private Instant endsAt;
  private Instant closedAt;
  private final boolean autoExtend;
  private final int extendMinutes;
  private final List<AuctionImage> images;
  private final Instant createdAt;
  private Instant updatedAt;
  private Long version;

  public static Auction create(
      UUID sellerId,
      UUID categoryId,
      String title,
      String description,
      BigDecimal startingPrice,
      BigDecimal reservePrice,
      Instant startsAt,
      Instant endsAt,
      boolean autoExtend,
      int extendMinutes) {
    validateCreation(title, startingPrice, reservePrice, startsAt, endsAt, extendMinutes);
    Instant now = Instant.now();
    return Auction.builder()
        .id(UUID.randomUUID())
        .sellerId(sellerId)
        .categoryId(categoryId)
        .title(title)
        .description(description)
        .startingPrice(startingPrice)
        .reservePrice(reservePrice)
        .currentPrice(startingPrice)
        .currentWinnerId(null)
        .status(AuctionStatus.DRAFT)
        .startsAt(startsAt)
        .endsAt(endsAt)
        .closedAt(null)
        .autoExtend(autoExtend)
        .extendMinutes(extendMinutes)
        .images(new ArrayList<>())
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  public static Auction reconstitute(
      UUID id,
      UUID sellerId,
      UUID categoryId,
      String title,
      String description,
      BigDecimal startingPrice,
      BigDecimal reservePrice,
      BigDecimal currentPrice,
      UUID currentWinnerId,
      AuctionStatus status,
      Instant startsAt,
      Instant endsAt,
      Instant closedAt,
      boolean autoExtend,
      int extendMinutes,
      List<AuctionImage> images,
      Instant createdAt,
      Instant updatedAt,
      Long version) {
    return Auction.builder()
        .id(id)
        .sellerId(sellerId)
        .categoryId(categoryId)
        .title(title)
        .description(description)
        .startingPrice(startingPrice)
        .reservePrice(reservePrice)
        .currentPrice(currentPrice)
        .currentWinnerId(currentWinnerId)
        .status(status)
        .startsAt(startsAt)
        .endsAt(endsAt)
        .closedAt(closedAt)
        .autoExtend(autoExtend)
        .extendMinutes(extendMinutes)
        .images(new ArrayList<>(images))
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .version(version)
        .build();
  }

  public void placeBid(UUID bidderId, BigDecimal amount) {
    if (!isAcceptingBids()) throw new AuctionExceptions.AuctionNotActiveException(id, status);
    if (bidderId.equals(sellerId)) throw new AuctionExceptions.SellerCannotBidException(id);
    if (amount.compareTo(currentPrice) <= 0)
      throw new AuctionExceptions.BidTooLowException(id, amount, currentPrice);
    this.currentPrice = amount;
    this.currentWinnerId = bidderId;
    this.updatedAt = Instant.now();
    if (autoExtend && isInLastMinutes()) extendTime();
  }

  public void publish() {
    if (status != AuctionStatus.DRAFT)
      throw new AuctionExceptions.InvalidAuctionStatusTransitionException(
          id, status, AuctionStatus.SCHEDULED);
    this.status = Instant.now().isBefore(startsAt) ? AuctionStatus.SCHEDULED : AuctionStatus.ACTIVE;
    this.updatedAt = Instant.now();
  }

  public void activate() {
    if (status != AuctionStatus.SCHEDULED)
      throw new AuctionExceptions.InvalidAuctionStatusTransitionException(
          id, status, AuctionStatus.ACTIVE);
    this.status = AuctionStatus.ACTIVE;
    this.updatedAt = Instant.now();
  }

  public void close() {
    if (status != AuctionStatus.ACTIVE && status != AuctionStatus.EXTENDED)
      throw new AuctionExceptions.InvalidAuctionStatusTransitionException(
          id, status, AuctionStatus.CLOSED);
    this.closedAt = Instant.now();
    this.updatedAt = Instant.now();
    boolean hasWinner = currentWinnerId != null;
    boolean reserveMet = reservePrice == null || currentPrice.compareTo(reservePrice) >= 0;
    this.status = (hasWinner && reserveMet) ? AuctionStatus.AWARDED : AuctionStatus.FAILED;
  }

  public void cancel() {
    if (status == AuctionStatus.AWARDED
        || status == AuctionStatus.PAID
        || status == AuctionStatus.FAILED)
      throw new AuctionExceptions.InvalidAuctionStatusTransitionException(
          id, status, AuctionStatus.CANCELLED);
    this.status = AuctionStatus.CANCELLED;
    this.updatedAt = Instant.now();
  }

  public void markAsPaid() {
    if (status != AuctionStatus.AWARDED)
      throw new AuctionExceptions.InvalidAuctionStatusTransitionException(
          id, status, AuctionStatus.PAID);
    this.status = AuctionStatus.PAID;
    this.updatedAt = Instant.now();
  }

  public List<AuctionImage> getImages() {
    return Collections.unmodifiableList(images);
  }

  public boolean isOwnedBy(UUID userId) {
    return sellerId.equals(userId);
  }

  public boolean hasWinner() {
    return currentWinnerId != null;
  }

  public boolean reservePriceMet() {
    return reservePrice == null || currentPrice.compareTo(reservePrice) >= 0;
  }

  private boolean isAcceptingBids() {
    return status == AuctionStatus.ACTIVE || status == AuctionStatus.EXTENDED;
  }

  private boolean isInLastMinutes() {
    return Instant.now().isAfter(endsAt.minusSeconds((long) extendMinutes * 60));
  }

  private void extendTime() {
    this.endsAt = endsAt.plusSeconds((long) extendMinutes * 60);
    this.status = AuctionStatus.EXTENDED;
  }

  private static void validateCreation(
      String title,
      BigDecimal startingPrice,
      BigDecimal reservePrice,
      Instant startsAt,
      Instant endsAt,
      int extendMinutes) {
    if (title == null || title.isBlank())
      throw new IllegalArgumentException("El titulo no puede estar vacio");
    if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0)
      throw new IllegalArgumentException("El precio inicial debe ser mayor a cero");
    if (reservePrice != null && reservePrice.compareTo(startingPrice) < 0)
      throw new IllegalArgumentException(
          "El precio de reserva no puede ser menor al precio inicial");
    if (endsAt == null || startsAt == null || !endsAt.isAfter(startsAt))
      throw new IllegalArgumentException("La fecha de cierre debe ser posterior a la de inicio");
    if (extendMinutes < 1 || extendMinutes > 60)
      throw new IllegalArgumentException("Los minutos de extension deben estar entre 1 y 60");
  }
}
