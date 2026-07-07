package exception;

import domain.user.UserExceptions;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserExceptionHandler {

  @ExceptionHandler(UserExceptions.EmailAlreadyTakenException.class)
  public ProblemDetail handleEmailTaken(UserExceptions.EmailAlreadyTakenException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setType(URI.create("urn:problem:email-already-taken"));
    problem.setTitle("Email en uso");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(UserExceptions.UsernameAlreadyTakenException.class)
  public ProblemDetail handleUsernameTaken(UserExceptions.UsernameAlreadyTakenException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setType(URI.create("urn:problem:username-already-taken"));
    problem.setTitle("Username en uso");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(UserExceptions.InvalidCredentialsException.class)
  public ProblemDetail handleInvalidCredentials(UserExceptions.InvalidCredentialsException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
    problem.setType(URI.create("urn:problem:invalid-credentials"));
    problem.setTitle("Credenciales inválidas");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(UserExceptions.InvalidRefreshTokenException.class)
  public ProblemDetail handleInvalidRefreshToken(UserExceptions.InvalidRefreshTokenException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
    problem.setType(URI.create("urn:problem:invalid-refresh-token"));
    problem.setTitle("Refresh token inválido");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(UserExceptions.EmailNotFoundException.class)
  public ProblemDetail handleEmailNotFound(UserExceptions.EmailNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setType(URI.create("urn:problem:email-not-found"));
    problem.setTitle("Email no encontrado");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(UserExceptions.UserNotFoundException.class)
  public ProblemDetail handleUserNotFound(UserExceptions.UserNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setType(URI.create("urn:problem:user-not-found"));
    problem.setTitle("Usuario no encontrado");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(UserExceptions.InvalidCurrentPasswordException.class)
  public ProblemDetail handleInvalidCurrentPassword(
      UserExceptions.InvalidCurrentPasswordException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    problem.setType(URI.create("urn:problem:invalid-current-password"));
    problem.setTitle("Contraseña actual incorrecta");
    problem.setDetail(ex.getMessage());
    return problem;
  }
}
