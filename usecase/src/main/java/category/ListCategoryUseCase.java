package category;

import category.output.GetCategoryResult;
import category.output.ListActiveCategoriesResult;
import domain.categories.Category;
import domain.categories.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class ListCategoryUseCase implements UseCase<String, ListActiveCategoriesResult> {

  private final CategoryRepository categoryRepository;

  @Override
  public ListActiveCategoriesResult execute(String input) {

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
  public ListActiveCategoriesResult failed(Exception exception) {
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al obtener la categoría", exception);
  }
}
