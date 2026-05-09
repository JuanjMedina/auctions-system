package domain.auction;

import java.util.UUID;

public class AuctionImage {

  private final UUID id;
  private final String url;
  private final boolean primary;
  private final int displayOrder;

  public AuctionImage(UUID id, String url, boolean primary, int displayOrder) {
    if (url == null || url.isBlank())
      throw new IllegalArgumentException("La URL no puede estar vacia");
    this.id = id;
    this.url = url;
    this.primary = primary;
    this.displayOrder = displayOrder;
  }

  public UUID getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public boolean isPrimary() {
    return primary;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }
}
