package exception;

import static org.assertj.core.api.Assertions.assertThat;

import domain.user.UserExceptions;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class UserExceptionHandlerTest {

  private final UserExceptionHandler handler = new UserExceptionHandler();

  @Test
  void handleEmailTaken_returnsConflictProblem() {
    String email = "test@test.com";
    UserExceptions.EmailAlreadyTakenException ex =
        new UserExceptions.EmailAlreadyTakenException(email);

    ProblemDetail problem = handler.handleEmailTaken(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getType()).hasToString("urn:problem:email-already-taken");
    assertThat(problem.getTitle()).isEqualTo("Email en uso");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleUsernameTaken_returnsConflictProblem() {
    String username = "juan123";
    UserExceptions.UsernameAlreadyTakenException ex =
        new UserExceptions.UsernameAlreadyTakenException(username);

    ProblemDetail problem = handler.handleUsernameTaken(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getType()).hasToString("urn:problem:username-already-taken");
    assertThat(problem.getTitle()).isEqualTo("Username en uso");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleInvalidCredentials_returnsUnauthorizedProblem() {
    UserExceptions.InvalidCredentialsException ex =
        new UserExceptions.InvalidCredentialsException("credenciales inválidas");

    ProblemDetail problem = handler.handleInvalidCredentials(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(problem.getType()).hasToString("urn:problem:invalid-credentials");
    assertThat(problem.getTitle()).isEqualTo("Credenciales inválidas");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleInvalidRefreshToken_returnsUnauthorizedProblem() {
    UserExceptions.InvalidRefreshTokenException ex =
        new UserExceptions.InvalidRefreshTokenException();

    ProblemDetail problem = handler.handleInvalidRefreshToken(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(problem.getType()).hasToString("urn:problem:invalid-refresh-token");
    assertThat(problem.getTitle()).isEqualTo("Refresh token inválido");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleEmailNotFound_returnsNotFoundProblem() {
    String email = "notfound@test.com";
    UserExceptions.EmailNotFoundException ex = new UserExceptions.EmailNotFoundException(email);

    ProblemDetail problem = handler.handleEmailNotFound(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getType()).hasToString("urn:problem:email-not-found");
    assertThat(problem.getTitle()).isEqualTo("Email no encontrado");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleUserNotFound_returnsNotFoundProblem() {
    UUID userId = UUID.randomUUID();
    UserExceptions.UserNotFoundException ex = new UserExceptions.UserNotFoundException(userId);

    ProblemDetail problem = handler.handleUserNotFound(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getType()).hasToString("urn:problem:user-not-found");
    assertThat(problem.getTitle()).isEqualTo("Usuario no encontrado");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleInvalidCurrentPassword_returnsUnprocessableEntityProblem() {
    UserExceptions.InvalidCurrentPasswordException ex =
        new UserExceptions.InvalidCurrentPasswordException();

    ProblemDetail problem = handler.handleInvalidCurrentPassword(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    assertThat(problem.getType()).hasToString("urn:problem:invalid-current-password");
    assertThat(problem.getTitle()).isEqualTo("Contraseña actual incorrecta");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }
}
