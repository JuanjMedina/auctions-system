package exception;

import domain.categories.CategoryExceptions;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CategoryExceptionHandler {

  @ExceptionHandler(CategoryExceptions.CategoryNotFoundException.class)
  public ProblemDetail handleCategoryNotFound(CategoryExceptions.CategoryNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setType(URI.create("urn:problem:category-not-found"));
    problem.setTitle("Categoría no encontrada");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(CategoryExceptions.CategorySlugAlreadyExistsException.class)
  public ProblemDetail handleSlugAlreadyExists(
      CategoryExceptions.CategorySlugAlreadyExistsException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setType(URI.create("urn:problem:category-slug-conflict"));
    problem.setTitle("Slug de categoría ya existe");
    problem.setDetail(ex.getMessage());
    problem.setProperty("slug", ex.getSlug());
    return problem;
  }
}
