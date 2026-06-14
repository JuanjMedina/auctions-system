package category;

import category.input.GetCategoryInput;
import category.output.GetCategoryResult;
import domain.categories.Category;
import domain.categories.CategoryExceptions;
import domain.categories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class GetCategoryUseCase implements UseCase<GetCategoryInput, GetCategoryResult> {

  private final CategoryRepository categoryRepository;

  @Override
  public GetCategoryResult execute(GetCategoryInput input) {
    Category category =
        categoryRepository
            .findById(input.categoryId())
            .orElseThrow(
                () -> new CategoryExceptions.CategoryNotFoundException(input.categoryId()));

    return new GetCategoryResult(
        category.getId(),
        category.getName(),
        category.getSlug(),
        category.getParentId(),
        category.isActive());
  }

  @Override
  public GetCategoryResult failed(Exception exception) {
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al obtener la categoría", exception);
  }
}
