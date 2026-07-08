package controller.category;

import category.CreateCategoryUseCase;
import category.GetCategoryUseCase;
import category.ListCategoriesUseCase;
import category.input.CreateCategoryInput;
import category.input.GetCategoryInput;
import category.output.CreateCategoryResult;
import category.output.GetCategoryResult;
import category.output.ListActiveCategoriesResult;
import controller.category.dto.CreateCategoryRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shared.NoInput;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CreateCategoryUseCase createCategoryUseCase;
  private final GetCategoryUseCase getCategoryUseCase;
  private final ListCategoriesUseCase listCategoriesUseCase;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CreateCategoryResult> create(
      @Valid @RequestBody CreateCategoryRequest request) {
    CreateCategoryInput input =
        new CreateCategoryInput(request.name(), request.slug(), request.parentId());
    return ResponseEntity.status(HttpStatus.CREATED).body(createCategoryUseCase.run(input));
  }

  @GetMapping("/{id}")
  public ResponseEntity<GetCategoryResult> get(@PathVariable UUID id) {
    return ResponseEntity.ok(getCategoryUseCase.run(new GetCategoryInput(id)));
  }

  @GetMapping
  public ResponseEntity<ListActiveCategoriesResult> listCategories() {
    return ResponseEntity.ok(listCategoriesUseCase.run(NoInput.INSTANCE));
  }
}
