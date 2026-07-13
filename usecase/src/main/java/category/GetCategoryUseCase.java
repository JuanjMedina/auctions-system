package category;

import category.input.GetCategoryInput;
import category.output.GetCategoryResult;
import domain.categories.Category;
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
    Category category = categoryRepository.getById(input.categoryId());

    return new GetCategoryResult(
        category.getId(),
        category.getName(),
        category.getSlug(),
        category.getParentId(),
        category.isActive());
  }

  @Override
  public String errorMessage() {
    return "Error al obtener la categoría";
  }
}
