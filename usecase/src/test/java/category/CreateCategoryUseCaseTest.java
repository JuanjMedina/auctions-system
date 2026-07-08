package category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import category.input.CreateCategoryInput;
import category.output.CreateCategoryResult;
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
class CreateCategoryUseCaseTest {

  @Mock private CategoryRepository categoryRepository;

  @InjectMocks private CreateCategoryUseCase useCase;

  // --- fixtures ---
  private static final String NAME = "Electronics";
  private static final String SLUG = "electronics";

  private CreateCategoryInput validInput(UUID parentId) {
    return new CreateCategoryInput(NAME, SLUG, parentId);
  }

  // --- happy path ---

  @Test
  void execute_newRootCategory_returnsCreatedCategory() {
    // arrange
    when(categoryRepository.existsBySlug(SLUG)).thenReturn(false);
    when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    CreateCategoryResult result = useCase.run(validInput(null));

    // assert
    assertThat(result.name()).isEqualTo(NAME);
    assertThat(result.slug()).isEqualTo(SLUG);
    assertThat(result.parentId()).isNull();
    assertThat(result.isActive()).isTrue();
    assertThat(result.id()).isNotNull();
  }

  @Test
  void execute_newSubCategory_returnsCreatedCategoryWithParentId() {
    // arrange
    UUID parentId = UUID.randomUUID();
    when(categoryRepository.existsBySlug(SLUG)).thenReturn(false);
    when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    CreateCategoryResult result = useCase.run(validInput(parentId));

    // assert
    assertThat(result.parentId()).isEqualTo(parentId);
  }

  @Test
  void execute_validInput_persistsCategory() {
    // arrange
    when(categoryRepository.existsBySlug(SLUG)).thenReturn(false);
    when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    useCase.run(validInput(null));

    // assert
    verify(categoryRepository).save(any(Category.class));
  }

  // --- slug ya existe ---

  @Test
  void execute_slugAlreadyExists_throwsCategorySlugAlreadyExistsException() {
    // arrange
    when(categoryRepository.existsBySlug(SLUG)).thenReturn(true);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput(null)))
        .isInstanceOf(CategoryExceptions.CategorySlugAlreadyExistsException.class);
  }

  @Test
  void execute_slugAlreadyExists_exceptionContainsSlug() {
    // arrange
    when(categoryRepository.existsBySlug(SLUG)).thenReturn(true);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput(null)))
        .isInstanceOf(CategoryExceptions.CategorySlugAlreadyExistsException.class)
        .hasMessageContaining(SLUG);
  }

  @Test
  void execute_slugAlreadyExists_neverSavesCategory() {
    // arrange
    when(categoryRepository.existsBySlug(SLUG)).thenReturn(true);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput(null)))
        .isInstanceOf(CategoryExceptions.CategorySlugAlreadyExistsException.class);

    verify(categoryRepository, never()).save(any());
  }
}
