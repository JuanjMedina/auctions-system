package user;

import domain.user.TokenGenerator;
import domain.user.User;
import domain.user.UserExceptions.InvalidCredentialsException;
import domain.user.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;
import user.input.RefreshTokenInput;
import user.output.LoginUserResult;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase implements UseCase<RefreshTokenInput, LoginUserResult> {

  private static final String TOKEN_TYPE = "Bearer";
  private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 86_400L; // 24h

  private final UserRepository userRepository;
  private final TokenGenerator tokenGenerator;

  @Override
  public LoginUserResult execute(RefreshTokenInput input) {
    UUID userId = tokenGenerator.extractUserIdFromRefreshToken(input.refreshToken());

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new InvalidCredentialsException("Credenciales inválidas"));

    String newAccessToken =
        tokenGenerator.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
    String newRefreshToken = tokenGenerator.generateRefreshToken(user.getId());

    return new LoginUserResult(
        newAccessToken, newRefreshToken, TOKEN_TYPE, ACCESS_TOKEN_EXPIRATION_SECONDS);
  }

  @Override
  public LoginUserResult failed(Exception exception) {
    if (exception instanceof InvalidCredentialsException e) throw e;
    if (exception instanceof domain.user.UserExceptions.InvalidRefreshTokenException e) throw e;
    throw new RuntimeException("Error inesperado en refresh token", exception);
  }
}
