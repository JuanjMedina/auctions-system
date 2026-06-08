package exception;

import domain.wallets.WalletExceptions;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WalletExceptionHandler {

  @ExceptionHandler(WalletExceptions.WalletNotFoundException.class)
  public ProblemDetail handleWalletNotFound(WalletExceptions.WalletNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setType(URI.create("urn:problem:wallet-not-found"));
    problem.setTitle("Billetera no encontrada");
    problem.setDetail(ex.getMessage());
    return problem;
  }

  @ExceptionHandler(WalletExceptions.InsufficientFundsException.class)
  public ProblemDetail handleInsufficientFunds(WalletExceptions.InsufficientFundsException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    problem.setType(URI.create("urn:problem:insufficient-funds"));
    problem.setTitle("Saldo insuficiente");
    problem.setDetail(ex.getMessage());
    problem.setProperty("available", ex.getAvailable());
    problem.setProperty("required", ex.getRequired());
    return problem;
  }
}
