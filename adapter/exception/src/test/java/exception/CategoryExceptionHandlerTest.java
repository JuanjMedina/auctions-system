package exception;

import static org.assertj.core.api.Assertions.assertThat;

import domain.categories.CategoryExceptions;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class CategoryExceptionHandlerTest {

  private final CategoryExceptionHandler handler = new CategoryExceptionHandler();

  @Test
  void handleCategoryNotFound_returnsNotFoundProblem() {
    UUID categoryId = UUID.randomUUID();
    CategoryExceptions.CategoryNotFoundException ex =
        new CategoryExceptions.CategoryNotFoundException(categoryId);

    ProblemDetail problem = handler.handleCategoryNotFound(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getType()).hasToString("urn:problem:category-not-found");
    assertThat(problem.getTitle()).isEqualTo("Categoría no encontrada");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleSlugAlreadyExists_returnsConflictProblemWithSlug() {
    String slug = "electronica";
    CategoryExceptions.CategorySlugAlreadyExistsException ex =
        new CategoryExceptions.CategorySlugAlreadyExistsException(slug);

    ProblemDetail problem = handler.handleSlugAlreadyExists(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getType()).hasToString("urn:problem:category-slug-conflict");
    assertThat(problem.getTitle()).isEqualTo("Slug de categoría ya existe");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
    assertThat(problem.getProperties()).containsEntry("slug", slug);
  }
}
