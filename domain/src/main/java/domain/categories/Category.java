package domain.categories;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = lombok.AccessLevel.PRIVATE)
public class Category {

  private final UUID id;
  private String name;
  private String slug;
  private final UUID parentId;
  private boolean isActive;

  public static Category create(String name, String slug, UUID parentId) {
    UUID id = UUID.randomUUID();
    if (id.equals(parentId)) {
      throw new IllegalArgumentException("A category cannot be its own parent");
    }
    return Category.builder()
        .id(id)
        .name(name)
        .slug(slug)
        .parentId(parentId)
        .isActive(true)
        .build();
  }

  public static Category reconstitute(
      UUID id, String name, String slug, UUID parentId, boolean isActive) {
    return Category.builder()
        .id(id)
        .name(name)
        .slug(slug)
        .parentId(parentId)
        .isActive(isActive)
        .build();
  }

  public boolean isRoot() {
    return parentId == null;
  }

  public void deactivate() {
    this.isActive = false;
  }

  public void activate() {
    this.isActive = true;
  }
}
