package exception;

import static org.assertj.core.api.Assertions.assertThat;

import domain.wallets.WalletExceptions;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class WalletExceptionHandlerTest {

  private final WalletExceptionHandler handler = new WalletExceptionHandler();

  @Test
  void handleWalletNotFound_returnsNotFoundProblem() {
    UUID userId = UUID.randomUUID();
    WalletExceptions.WalletNotFoundException ex =
        new WalletExceptions.WalletNotFoundException(userId);

    ProblemDetail problem = handler.handleWalletNotFound(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getType()).hasToString("urn:problem:wallet-not-found");
    assertThat(problem.getTitle()).isEqualTo("Billetera no encontrada");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
  }

  @Test
  void handleInsufficientFunds_returnsUnprocessableEntityProblemWithAmounts() {
    BigDecimal available = BigDecimal.valueOf(10);
    BigDecimal required = BigDecimal.valueOf(50);
    WalletExceptions.InsufficientFundsException ex =
        new WalletExceptions.InsufficientFundsException(available, required);

    ProblemDetail problem = handler.handleInsufficientFunds(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    assertThat(problem.getType()).hasToString("urn:problem:insufficient-funds");
    assertThat(problem.getTitle()).isEqualTo("Saldo insuficiente");
    assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
    assertThat(problem.getProperties()).containsEntry("available", available);
    assertThat(problem.getProperties()).containsEntry("required", required);
  }
}
