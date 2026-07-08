package exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import domain.shared.ConcurrencyException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
class ValidationExceptionHandlerTest {

  private final ValidationExceptionHandler handler = new ValidationExceptionHandler();

  @Mock private MethodParameter methodParameter;
  @Mock private BindingResult bindingResult;

  @Test
  void handleConcurrency_returnsConflictProblem() {
    ConcurrencyException ex = new ConcurrencyException("Auction", java.util.UUID.randomUUID());

    ProblemDetail problem = handler.handleConcurrency(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getType()).hasToString("urn:problem:concurrency-conflict");
    assertThat(problem.getTitle()).isEqualTo("Conflicto de concurrencia");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleValidation_withFieldErrors_returnsBadRequestWithJoinedDetail() {
    FieldError fieldError1 = new FieldError("request", "email", "no debe estar vacío");
    FieldError fieldError2 = new FieldError("request", "password", "debe tener 8 caracteres");
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

    MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(methodParameter, bindingResult);

    ProblemDetail problem = handler.handleValidation(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getType()).hasToString("urn:problem:validation-error");
    assertThat(problem.getTitle()).isEqualTo("Error de validación");
    assertThat(problem.getDetail())
        .isEqualTo("email: no debe estar vacío; password: debe tener 8 caracteres");
  }

  @Test
  void handleValidation_withNoFieldErrors_returnsDefaultDetail() {
    when(bindingResult.getFieldErrors()).thenReturn(List.of());

    MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(methodParameter, bindingResult);

    ProblemDetail problem = handler.handleValidation(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getDetail()).isEqualTo("Datos inválidos");
  }

  @Test
  void handleIllegalArgument_returnsBadRequestProblem() {
    IllegalArgumentException ex = new IllegalArgumentException("argumento inválido");

    ProblemDetail problem = handler.handleIllegalArgument(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getType()).hasToString("urn:problem:invalid-argument");
    assertThat(problem.getTitle()).isEqualTo("Argumento inválido");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleUnexpected_returnsInternalServerErrorProblem() {
    Exception ex = new RuntimeException("boom");

    ProblemDetail problem = handler.handleUnexpected(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(problem.getType()).hasToString("urn:problem:internal-error");
    assertThat(problem.getTitle()).isEqualTo("Error interno del servidor");
    assertThat(problem.getDetail()).isEqualTo("Ocurrió un error inesperado");
  }
}
