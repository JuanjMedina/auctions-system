package exception;

import domain.shared.ConcurrencyException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ValidationExceptionHandler {

  @ExceptionHandler(ConcurrencyException.class)
  public ProblemDetail handleConcurrency(ConcurrencyException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setType(URI.create("urn:problem:concurrency-conflict"));
    problem.setTitle("Conflicto de concurrencia");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setType(URI.create("urn:problem:validation-error"));
    problem.setTitle("Error de validación");
    String detail =
        ex.getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Datos inválidos");
    problem.setDetail(detail);
    return problem;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setType(URI.create("urn:problem:invalid-argument"));
    problem.setTitle("Argumento inválido");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex) {
    log.error("Excepción no controlada: {}", ex.getMessage(), ex);
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    problem.setType(URI.create("urn:problem:internal-error"));
    problem.setTitle("Error interno del servidor");
    problem.setDetail("Ocurrió un error inesperado");
    return problem;
  }
}
