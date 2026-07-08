package exception;

import static org.assertj.core.api.Assertions.assertThat;

import domain.watchList.WatchListExceptions;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class WatchListExceptionHandlerTest {

  private final WatchListExceptionHandler handler = new WatchListExceptionHandler();

  @Test
  void handleAlreadyInWatchList_returnsConflictProblem() {
    UUID userId = UUID.randomUUID();
    UUID auctionId = UUID.randomUUID();
    WatchListExceptions.AlreadyInWatchListException ex =
        new WatchListExceptions.AlreadyInWatchListException(userId, auctionId);

    ProblemDetail problem = handler.handleAlreadyInWatchList(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getType()).hasToString("urn:problem:already-in-watchlist");
    assertThat(problem.getTitle()).isEqualTo("Subasta ya esta en favoritos");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleWatchListEntryNotFound_returnsNotFoundProblem() {
    UUID userId = UUID.randomUUID();
    UUID auctionId = UUID.randomUUID();
    WatchListExceptions.WatchListEntryNotFoundException ex =
        new WatchListExceptions.WatchListEntryNotFoundException(userId, auctionId);

    ProblemDetail problem = handler.handleWatchListEntryNotFound(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getType()).hasToString("urn:problem:watchlist-entry-not-found");
    assertThat(problem.getTitle()).isEqualTo("Favorito no encontrado");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }
}
