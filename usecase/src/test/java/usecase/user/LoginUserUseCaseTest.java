package usecase.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import domain.user.Role;
import domain.user.TokenGenerator;
import domain.user.User;
import domain.user.UserExceptions.InvalidCredentialsException;
import domain.user.UserPasswordEncoder;
import domain.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import usecase.user.input.LoginUserInput;
import usecase.user.output.LoginUserResult;

@ExtendWith(MockitoExtension.class)
class LoginUserUseCaseTest {

  @Mock private UserRepository userRepository;
  @Mock private UserPasswordEncoder passwordEncoder;
  @Mock private TokenGenerator tokenGenerator;

  @InjectMocks private LoginUserUseCase useCase;

  // --- fixtures ---
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String EMAIL = "juan@test.com";
  private static final String RAW_PASSWORD = "password123";
  private static final String HASH = "$2a$12$hashedpassword";
  private static final String ACCESS_TOKEN = "access.token.jwt";
  private static final String REFRESH_TOKEN = "refresh.token.jwt";

  private User buildUser() {
    return User.reconstitute(
        USER_ID,
        EMAIL,
        "juanito",
        HASH,
        "Juan Test",
        "555-0000",
        Role.BUYER,
        true,
        Instant.now(),
        Instant.now());
  }

  private LoginUserInput validInput() {
    return new LoginUserInput(null, EMAIL, RAW_PASSWORD);
  }

  // --- happy path ---

  @Test
  void execute_validCredentials_returnsTokenPair() {
    User user = buildUser();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(RAW_PASSWORD, HASH)).thenReturn(true);
    when(tokenGenerator.generateAccessToken(USER_ID, EMAIL, Role.BUYER)).thenReturn(ACCESS_TOKEN);
    when(tokenGenerator.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);

    LoginUserResult result = useCase.execute(validInput());

    assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
    assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
    assertThat(result.tokenType()).isEqualTo("Bearer");
    assertThat(result.expiresIn()).isEqualTo(86_400L);
  }

  @Test
  void execute_validCredentials_accessAndRefreshTokensAreDifferent() {
    User user = buildUser();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(RAW_PASSWORD, HASH)).thenReturn(true);
    when(tokenGenerator.generateAccessToken(USER_ID, EMAIL, Role.BUYER)).thenReturn(ACCESS_TOKEN);
    when(tokenGenerator.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);

    LoginUserResult result = useCase.execute(validInput());

    assertThat(result.accessToken()).isNotEqualTo(result.refreshToken());
  }

  @Test
  void execute_validCredentials_callsTokenGeneratorWithCorrectUserData() {
    User user = buildUser();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(RAW_PASSWORD, HASH)).thenReturn(true);
    when(tokenGenerator.generateAccessToken(USER_ID, EMAIL, Role.BUYER)).thenReturn(ACCESS_TOKEN);
    when(tokenGenerator.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);

    useCase.execute(validInput());

    verify(tokenGenerator).generateAccessToken(USER_ID, EMAIL, Role.BUYER);
    verify(tokenGenerator).generateRefreshToken(USER_ID);
  }

  // --- email no encontrado ---

  @Test
  void execute_emailNotFound_throwsInvalidCredentialsException() {
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(validInput()))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void execute_emailNotFound_neverChecksPasswordOrGeneratesToken() {
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(validInput()))
        .isInstanceOf(InvalidCredentialsException.class);

    verifyNoInteractions(passwordEncoder, tokenGenerator);
  }

  // --- password incorrecto ---

  @Test
  void execute_wrongPassword_throwsInvalidCredentialsException() {
    User user = buildUser();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(RAW_PASSWORD, HASH)).thenReturn(false);

    assertThatThrownBy(() -> useCase.execute(validInput()))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void execute_wrongPassword_neverGeneratesToken() {
    User user = buildUser();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(RAW_PASSWORD, HASH)).thenReturn(false);

    assertThatThrownBy(() -> useCase.execute(validInput()))
        .isInstanceOf(InvalidCredentialsException.class);

    verifyNoInteractions(tokenGenerator);
  }

  // --- seguridad: mensaje genérico en ambos casos ---

  @Test
  void execute_emailNotFound_sameMessageAsWrongPassword() {
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(validInput()))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Credenciales inválidas");
  }

  @Test
  void execute_wrongPassword_sameMessageAsEmailNotFound() {
    User user = buildUser();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(RAW_PASSWORD, HASH)).thenReturn(false);

    assertThatThrownBy(() -> useCase.execute(validInput()))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Credenciales inválidas");
  }
}
