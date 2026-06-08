package exception;

import domain.auction.AuctionExceptions;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuctionExceptionHandler {

  @ExceptionHandler(AuctionExceptions.AuctionNotFoundException.class)
  public ProblemDetail handleAuctionNotFound(AuctionExceptions.AuctionNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setType(URI.create("urn:problem:auction-not-found"));
    problem.setTitle("Subasta no encontrada");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(AuctionExceptions.UnauthorizedAuctionAccessException.class)
  public ProblemDetail handleUnauthorizedAccess(
      AuctionExceptions.UnauthorizedAuctionAccessException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    problem.setType(URI.create("urn:problem:unauthorized-auction-access"));
    problem.setTitle("Acceso no autorizado");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(AuctionExceptions.SellerCannotBidException.class)
  public ProblemDetail handleSellerCannotBid(AuctionExceptions.SellerCannotBidException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    problem.setType(URI.create("urn:problem:seller-cannot-bid"));
    problem.setTitle("Operación no permitida");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(AuctionExceptions.AuctionNotActiveException.class)
  public ProblemDetail handleAuctionNotActive(AuctionExceptions.AuctionNotActiveException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setType(URI.create("urn:problem:auction-not-active"));
    problem.setTitle("Subasta no activa");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(AuctionExceptions.InvalidAuctionStatusTransitionException.class)
  public ProblemDetail handleInvalidTransition(
      AuctionExceptions.InvalidAuctionStatusTransitionException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setType(URI.create("urn:problem:invalid-auction-status"));
    problem.setTitle("Transición de estado inválida");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(AuctionExceptions.AuctionAlreadyClosedException.class)
  public ProblemDetail handleAuctionAlreadyClosed(
      AuctionExceptions.AuctionAlreadyClosedException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setType(URI.create("urn:problem:auction-already-closed"));
    problem.setTitle("Subasta ya cerrada");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(AuctionExceptions.BidTooLowException.class)
  public ProblemDetail handleBidTooLow(AuctionExceptions.BidTooLowException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    problem.setType(URI.create("urn:problem:bid-too-low"));
    problem.setTitle("Puja insuficiente");
    problem.setDetail(ex.getMessage());
    problem.setProperty("currentPrice", ex.getCurrentPrice());
    problem.setProperty("attemptedAmount", ex.getAttemptedAmount());
    return problem;
  }
}
