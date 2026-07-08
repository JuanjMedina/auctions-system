package exception;

import static org.assertj.core.api.Assertions.assertThat;

import domain.bid.BidExceptions;
import domain.bid.BidStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class BidExceptionHandlerTest {

  private final BidExceptionHandler handler = new BidExceptionHandler();

  @Test
  void handleBidNotFound_returnsNotFoundProblem() {
    UUID bidId = UUID.randomUUID();
    BidExceptions.BidNotFoundException ex = new BidExceptions.BidNotFoundException(bidId);

    ProblemDetail problem = handler.handleBidNotFound(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getType()).hasToString("urn:problem:bid-not-found");
    assertThat(problem.getTitle()).isEqualTo("Puja no encontrada");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleBidAmountTooLow_returnsUnprocessableEntityProblem() {
    UUID auctionId = UUID.randomUUID();
    BidExceptions.BidAmountTooLowException ex =
        new BidExceptions.BidAmountTooLowException(auctionId, "monto insuficiente");

    ProblemDetail problem = handler.handleBidAmountTooLow(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    assertThat(problem.getType()).hasToString("urn:problem:bid-amount-too-low");
    assertThat(problem.getTitle()).isEqualTo("Monto de puja inválido");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleInvalidBidTransition_returnsConflictProblem() {
    UUID bidId = UUID.randomUUID();
    BidExceptions.InvalidBidStatusTransitionException ex =
        new BidExceptions.InvalidBidStatusTransitionException(
            bidId, BidStatus.ACTIVE, BidStatus.WINNING);

    ProblemDetail problem = handler.handleInvalidBidTransition(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getType()).hasToString("urn:problem:invalid-bid-status");
    assertThat(problem.getTitle()).isEqualTo("Transición de puja inválida");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }
}
