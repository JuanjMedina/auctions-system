package exception;

import domain.watchList.WatchListExceptions;
import java.net.URI;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(0)
public class WatchListExceptionHandler {

  @ExceptionHandler(WatchListExceptions.AlreadyInWatchListException.class)
  public ProblemDetail handleAlreadyInWatchList(
      WatchListExceptions.AlreadyInWatchListException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setType(URI.create("urn:problem:already-in-watchlist"));
    problem.setTitle("Subasta ya esta en favoritos");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(WatchListExceptions.WatchListEntryNotFoundException.class)
  public ProblemDetail handleWatchListEntryNotFound(
      WatchListExceptions.WatchListEntryNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setType(URI.create("urn:problem:watchlist-entry-not-found"));
    problem.setTitle("Favorito no encontrado");
    problem.setDetail(ex.getMessage());
    return problem;
  }
}
