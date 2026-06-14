package category.input;

import java.util.UUID;

public record CreateCategoryInput(String name, String slug, UUID parentId) {}
