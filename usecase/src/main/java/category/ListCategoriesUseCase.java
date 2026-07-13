package category;

import category.output.GetCategoryResult;
import category.output.ListActiveCategoriesResult;
import domain.categories.Category;
import domain.categories.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.NoInput;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class ListCategoriesUseCase implements UseCase<NoInput, ListActiveCategoriesResult> {

  private final CategoryRepository categoryRepository;

  @Override
  public ListActiveCategoriesResult execute(NoInput input) {

    List<Category> categoriesList = categoryRepository.findAllActive();

    return toResult(categoriesList);
  }

  private ListActiveCategoriesResult toResult(List<Category> categories) {
    List<GetCategoryResult> categoriesMap =
        categories.stream()
            .map(
                category ->
                    new GetCategoryResult(
                        category.getId(),
                        category.getName(),
                        category.getSlug(),
                        category.getParentId(),
                        category.isActive()))
            .toList();

    return new ListActiveCategoriesResult(categoriesMap);
  }

  @Override
  public String errorMessage() {
    return "Error al obtener las categorias";
  }
}
