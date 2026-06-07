package user;

import domain.user.TokenGenerator;
import domain.user.User;
import domain.user.UserExceptions.InvalidCredentialsException;
import domain.user.UserPasswordEncoder;
import domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;
import user.input.LoginUserInput;
import user.output.LoginUserResult;

@Service
@RequiredArgsConstructor
public class LoginUserUseCase implements UseCase<LoginUserInput, LoginUserResult> {

  private static final String TOKEN_TYPE = "Bearer";
  private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 86_400L; // 24h

  private final UserRepository userRepository;
  private final UserPasswordEncoder passwordEncoder;
  private final TokenGenerator tokenGenerator;

  @Override
  public LoginUserResult execute(LoginUserInput input) {
    User user =
        userRepository
            .findByEmail(input.email())
            .orElseThrow(() -> new InvalidCredentialsException("Credenciales inválidas"));

    if (!passwordEncoder.matches(input.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException("Credenciales inválidas");
    }

    String accessToken =
        tokenGenerator.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
    String refreshToken = tokenGenerator.generateRefreshToken(user.getId());

    return new LoginUserResult(
        accessToken, refreshToken, TOKEN_TYPE, ACCESS_TOKEN_EXPIRATION_SECONDS);
  }

  @Override
  public LoginUserResult failed(Exception exception) {
    if (exception instanceof InvalidCredentialsException e) throw e;
    throw new RuntimeException("Error inesperado en login", exception);
  }
}
