package category;

import category.input.CreateCategoryInput;
import category.output.CreateCategoryResult;
import domain.categories.Category;
import domain.categories.CategoryExceptions;
import domain.categories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class CreateCategoryUseCase implements UseCase<CreateCategoryInput, CreateCategoryResult> {

  private final CategoryRepository categoryRepository;

  @Override
  @Transactional
  public CreateCategoryResult execute(CreateCategoryInput input) {
    if (categoryRepository.existsBySlug(input.slug())) {
      throw new CategoryExceptions.CategorySlugAlreadyExistsException(input.slug());
    }

    Category category = Category.create(input.name(), input.slug(), input.parentId());
    Category saved = categoryRepository.save(category);

    return new CreateCategoryResult(
        saved.getId(), saved.getName(), saved.getSlug(), saved.getParentId(), saved.isActive());
  }

  @Override
  public CreateCategoryResult failed(Exception exception) {
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al crear la categoría", exception);
  }
}
