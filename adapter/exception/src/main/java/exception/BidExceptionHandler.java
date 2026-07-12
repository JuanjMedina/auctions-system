package exception;

import domain.bid.BidExceptions;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BidExceptionHandler {

  @ExceptionHandler(BidExceptions.BidNotFoundException.class)
  public ProblemDetail handleBidNotFound(BidExceptions.BidNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setType(URI.create("urn:problem:bid-not-found"));
    problem.setTitle("Puja no encontrada");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(BidExceptions.BidAmountTooLowException.class)
  public ProblemDetail handleBidAmountTooLow(BidExceptions.BidAmountTooLowException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    problem.setType(URI.create("urn:problem:bid-amount-too-low"));
    problem.setTitle("Monto de puja inválido");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(BidExceptions.InvalidBidStatusTransitionException.class)
  public ProblemDetail handleInvalidBidTransition(
      BidExceptions.InvalidBidStatusTransitionException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setType(URI.create("urn:problem:invalid-bid-status"));
    problem.setTitle("Transición de puja inválida");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(BidExceptions.UnauthorizedBidAccessException.class)
  public ProblemDetail handleUnauthorizedBidAccess(
      BidExceptions.UnauthorizedBidAccessException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    problem.setType(URI.create("urn:problem:unauthorized-bid-access"));
    problem.setTitle("Acceso no autorizado");
    problem.setDetail(ex.getMessage());
    return problem;
  }
}
