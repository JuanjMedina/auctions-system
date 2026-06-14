package domain.categories;

import java.util.UUID;

public final class CategoryExceptions {

  private CategoryExceptions() {}

  public static class CategoryNotFoundException extends RuntimeException {
    private final UUID categoryId;

    public CategoryNotFoundException(UUID categoryId) {
      super("Categoría no encontrada: " + categoryId);
      this.categoryId = categoryId;
    }

    public UUID getCategoryId() {
      return categoryId;
    }
  }

  public static class CategorySlugAlreadyExistsException extends RuntimeException {
    private final String slug;

    public CategorySlugAlreadyExistsException(String slug) {
      super("Ya existe una categoría con el slug: " + slug);
      this.slug = slug;
    }

    public String getSlug() {
      return slug;
    }
  }
}
