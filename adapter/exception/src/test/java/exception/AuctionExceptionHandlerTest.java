package exception;

import static org.assertj.core.api.Assertions.assertThat;

import domain.auction.AuctionExceptions;
import domain.auction.AuctionStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class AuctionExceptionHandlerTest {

  private final AuctionExceptionHandler handler = new AuctionExceptionHandler();

  @Test
  void handleAuctionNotFound_returnsNotFoundProblem() {
    UUID auctionId = UUID.randomUUID();
    AuctionExceptions.AuctionNotFoundException ex =
        new AuctionExceptions.AuctionNotFoundException(auctionId);

    ProblemDetail problem = handler.handleAuctionNotFound(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getType()).hasToString("urn:problem:auction-not-found");
    assertThat(problem.getTitle()).isEqualTo("Subasta no encontrada");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleUnauthorizedAccess_returnsForbiddenProblem() {
    UUID auctionId = UUID.randomUUID();
    AuctionExceptions.UnauthorizedAuctionAccessException ex =
        new AuctionExceptions.UnauthorizedAuctionAccessException(auctionId);

    ProblemDetail problem = handler.handleUnauthorizedAccess(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(problem.getType()).hasToString("urn:problem:unauthorized-auction-access");
    assertThat(problem.getTitle()).isEqualTo("Acceso no autorizado");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleSellerCannotBid_returnsForbiddenProblem() {
    UUID auctionId = UUID.randomUUID();
    AuctionExceptions.SellerCannotBidException ex =
        new AuctionExceptions.SellerCannotBidException(auctionId);

    ProblemDetail problem = handler.handleSellerCannotBid(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(problem.getType()).hasToString("urn:problem:seller-cannot-bid");
    assertThat(problem.getTitle()).isEqualTo("Operación no permitida");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleAuctionNotActive_returnsConflictProblem() {
    UUID auctionId = UUID.randomUUID();
    AuctionExceptions.AuctionNotActiveException ex =
        new AuctionExceptions.AuctionNotActiveException(auctionId, AuctionStatus.CLOSED);

    ProblemDetail problem = handler.handleAuctionNotActive(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getType()).hasToString("urn:problem:auction-not-active");
    assertThat(problem.getTitle()).isEqualTo("Subasta no activa");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleInvalidTransition_returnsConflictProblem() {
    UUID auctionId = UUID.randomUUID();
    AuctionExceptions.InvalidAuctionStatusTransitionException ex =
        new AuctionExceptions.InvalidAuctionStatusTransitionException(
            auctionId, AuctionStatus.DRAFT, AuctionStatus.CLOSED);

    ProblemDetail problem = handler.handleInvalidTransition(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getType()).hasToString("urn:problem:invalid-auction-status");
    assertThat(problem.getTitle()).isEqualTo("Transición de estado inválida");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleAuctionAlreadyClosed_returnsConflictProblem() {
    UUID auctionId = UUID.randomUUID();
    AuctionExceptions.AuctionAlreadyClosedException ex =
        new AuctionExceptions.AuctionAlreadyClosedException(auctionId);

    ProblemDetail problem = handler.handleAuctionAlreadyClosed(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getType()).hasToString("urn:problem:auction-already-closed");
    assertThat(problem.getTitle()).isEqualTo("Subasta ya cerrada");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleBidTooLow_returnsUnprocessableEntityProblemWithAmounts() {
    UUID auctionId = UUID.randomUUID();
    BigDecimal attempted = BigDecimal.valueOf(10);
    BigDecimal current = BigDecimal.valueOf(20);
    AuctionExceptions.BidTooLowException ex =
        new AuctionExceptions.BidTooLowException(auctionId, attempted, current);

    ProblemDetail problem = handler.handleBidTooLow(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    assertThat(problem.getType()).hasToString("urn:problem:bid-too-low");
    assertThat(problem.getTitle()).isEqualTo("Puja insuficiente");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
    assertThat(problem.getProperties()).containsEntry("currentPrice", current);
    assertThat(problem.getProperties()).containsEntry("attemptedAmount", attempted);
  }
}
