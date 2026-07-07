package domain.user;

import java.util.UUID;

public final class UserExceptions {

  private UserExceptions() {}

  public static class EmailAlreadyTakenException extends RuntimeException {
    private final String email;

    public EmailAlreadyTakenException(String email) {
      super("El email ya está registrado: " + email);
      this.email = email;
    }

    public String getEmail() {
      return email;
    }
  }

  public static class UsernameAlreadyTakenException extends RuntimeException {
    private final String username;

    public UsernameAlreadyTakenException(String username) {
      super("El nombre de usuario ya está en uso: " + username);
      this.username = username;
    }

    public String getUsername() {
      return username;
    }
  }

  public static class EmailNotFoundException extends RuntimeException {
    private final String email;

    public EmailNotFoundException(String email) {
      super("El email no se encontró: " + email);
      this.email = email;
    }

    public String getEmail() {
      return email;
    }
  }

  public static class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
      super(message);
    }
  }

  public static class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
      super("El refresh token es inválido o ha expirado");
    }
  }

  public static class UserNotFoundException extends RuntimeException {
    private final UUID userId;

    public UserNotFoundException(UUID userId) {
      super("Usuario no encontrado: " + userId);
      this.userId = userId;
    }

    public UUID getUserId() {
      return userId;
    }
  }

  public static class InvalidCurrentPasswordException extends RuntimeException {
    public InvalidCurrentPasswordException() {
      super("La contraseña actual no es correcta");
    }
  }
}
