package domain.wallets;

import java.math.BigDecimal;
import java.util.UUID;

public final class WalletExceptions {

  private WalletExceptions() {}

  public static class WalletNotFoundException extends RuntimeException {
    private final UUID userId;

    public WalletNotFoundException(UUID userId) {
      super("Billetera no encontrada para el usuario: " + userId);
      this.userId = userId;
    }

    public UUID getUserId() {
      return userId;
    }
  }

  public static class InsufficientFundsException extends RuntimeException {
    private final BigDecimal available;
    private final BigDecimal required;

    public InsufficientFundsException(BigDecimal available, BigDecimal required) {
      super(
          String.format(
              "Saldo disponible insuficiente: disponible %.2f, requerido %.2f",
              available, required));
      this.available = available;
      this.required = required;
    }

    public BigDecimal getAvailable() {
      return available;
    }

    public BigDecimal getRequired() {
      return required;
    }
  }
}
