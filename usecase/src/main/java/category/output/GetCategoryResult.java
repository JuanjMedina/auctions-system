package category.output;

import java.util.UUID;

public record GetCategoryResult(
    UUID id, String name, String slug, UUID parentId, boolean isActive) {}
