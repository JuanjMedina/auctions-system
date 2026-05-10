package domain.categories;

import java.util.UUID;
import lombok.Getter;

@Getter
public class Category {

  private final UUID id;
  private String name;
  private String slug;
  private final UUID parentId; // null = categoría raíz
  private boolean isActive;

  private Category(UUID id, String name, String slug, UUID parentId, boolean isActive) {
    if (id.equals(parentId)) {
      throw new IllegalArgumentException("A category cannot be its own parent");
    }
    this.id = id;
    this.name = name;
    this.slug = slug;
    this.parentId = parentId;
    this.isActive = isActive;
  }

  public static Category create(String name, String slug, UUID parentId) {
    return new Category(UUID.randomUUID(), name, slug, parentId, true);
  }

  public static Category reconstitute(
      UUID id, String name, String slug, UUID parentId, boolean isActive) {
    return new Category(id, name, slug, parentId, isActive);
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
