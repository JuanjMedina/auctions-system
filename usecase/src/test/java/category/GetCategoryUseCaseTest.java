package category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import category.input.GetCategoryInput;
import category.output.GetCategoryResult;
import domain.categories.Category;
import domain.categories.CategoryExceptions;
import domain.categories.CategoryRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCategoryUseCaseTest {

  @Mock private CategoryRepository categoryRepository;

  @InjectMocks private GetCategoryUseCase useCase;

  // --- fixtures ---
  private static final UUID CATEGORY_ID = UUID.randomUUID();

  private Category buildCategory(UUID parentId, boolean active) {
    return Category.reconstitute(CATEGORY_ID, "Electronics", "electronics", parentId, active);
  }

  // --- happy path ---

  @Test
  void execute_existingCategory_returnsCategoryData() {
    // arrange
    Category category = buildCategory(null, true);
    when(categoryRepository.getById(CATEGORY_ID)).thenReturn(category);

    // act
    GetCategoryResult result = useCase.run(new GetCategoryInput(CATEGORY_ID));

    // assert
    assertThat(result.id()).isEqualTo(CATEGORY_ID);
    assertThat(result.name()).isEqualTo("Electronics");
    assertThat(result.slug()).isEqualTo("electronics");
    assertThat(result.parentId()).isNull();
    assertThat(result.isActive()).isTrue();
  }

  @Test
  void execute_existingSubCategory_returnsParentId() {
    // arrange
    UUID parentId = UUID.randomUUID();
    Category category = buildCategory(parentId, true);
    when(categoryRepository.getById(CATEGORY_ID)).thenReturn(category);

    // act
    GetCategoryResult result = useCase.run(new GetCategoryInput(CATEGORY_ID));

    // assert
    assertThat(result.parentId()).isEqualTo(parentId);
  }

  @Test
  void execute_inactiveCategory_returnsIsActiveFalse() {
    // arrange
    Category category = buildCategory(null, false);
    when(categoryRepository.getById(CATEGORY_ID)).thenReturn(category);

    // act
    GetCategoryResult result = useCase.run(new GetCategoryInput(CATEGORY_ID));

    // assert
    assertThat(result.isActive()).isFalse();
  }

  // --- categoría no encontrada ---

  @Test
  void execute_categoryNotFound_throwsCategoryNotFoundException() {
    // arrange
    when(categoryRepository.getById(CATEGORY_ID))
        .thenThrow(new CategoryExceptions.CategoryNotFoundException(CATEGORY_ID));

    // act & assert
    assertThatThrownBy(() -> useCase.run(new GetCategoryInput(CATEGORY_ID)))
        .isInstanceOf(CategoryExceptions.CategoryNotFoundException.class);
  }

  @Test
  void execute_categoryNotFound_exceptionContainsCategoryId() {
    // arrange
    when(categoryRepository.getById(CATEGORY_ID))
        .thenThrow(new CategoryExceptions.CategoryNotFoundException(CATEGORY_ID));

    // act & assert
    assertThatThrownBy(() -> useCase.run(new GetCategoryInput(CATEGORY_ID)))
        .isInstanceOf(CategoryExceptions.CategoryNotFoundException.class)
        .hasMessageContaining(CATEGORY_ID.toString());
  }
}
