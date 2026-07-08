package category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import category.output.GetCategoryResult;
import category.output.ListActiveCategoriesResult;
import domain.categories.Category;
import domain.categories.CategoryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shared.NoInput;

@ExtendWith(MockitoExtension.class)
class ListCategoriesUseCaseTest {

  @Mock private CategoryRepository categoryRepository;

  @InjectMocks private ListCategoriesUseCase useCase;

  // --- happy path ---

  @Test
  void execute_noActiveCategories_returnsEmptyList() {
    // arrange
    when(categoryRepository.findAllActive()).thenReturn(List.of());

    // act
    ListActiveCategoriesResult result = useCase.run(NoInput.INSTANCE);

    // assert
    assertThat(result.categories()).isEmpty();
  }

  @Test
  void execute_activeCategories_returnsMappedResults() {
    // arrange
    UUID electronicsId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    Category electronics =
        Category.reconstitute(electronicsId, "Electronics", "electronics", null, true);
    Category phones = Category.reconstitute(UUID.randomUUID(), "Phones", "phones", parentId, true);
    when(categoryRepository.findAllActive()).thenReturn(List.of(electronics, phones));

    // act
    ListActiveCategoriesResult result = useCase.run(NoInput.INSTANCE);

    // assert
    assertThat(result.categories()).hasSize(2);
    GetCategoryResult first = result.categories().get(0);
    assertThat(first.id()).isEqualTo(electronicsId);
    assertThat(first.name()).isEqualTo("Electronics");
    assertThat(first.slug()).isEqualTo("electronics");
    assertThat(first.parentId()).isNull();
    assertThat(first.isActive()).isTrue();

    GetCategoryResult second = result.categories().get(1);
    assertThat(second.name()).isEqualTo("Phones");
    assertThat(second.parentId()).isEqualTo(parentId);
  }

  @Test
  void execute_repositoryThrowsRuntimeException_propagatesAsIs() {
    // arrange
    when(categoryRepository.findAllActive()).thenThrow(new IllegalStateException("db down"));

    // act & assert
    assertThatThrownBy(() -> useCase.run(NoInput.INSTANCE))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("db down");
  }
}
