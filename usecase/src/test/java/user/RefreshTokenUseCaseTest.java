package user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import domain.user.Role;
import domain.user.TokenGenerator;
import domain.user.User;
import domain.user.UserExceptions.InvalidCredentialsException;
import domain.user.UserExceptions.InvalidRefreshTokenException;
import domain.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import user.input.RefreshTokenInput;
import user.output.LoginUserResult;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

  @Mock private UserRepository userRepository;
  @Mock private TokenGenerator tokenGenerator;

  @InjectMocks private RefreshTokenUseCase useCase;

  // --- fixtures ---
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String EMAIL = "juan@test.com";
  private static final String REFRESH_TOKEN = "valid.refresh.token";
  private static final String NEW_ACCESS_TOKEN = "new.access.token";
  private static final String NEW_REFRESH_TOKEN = "new.refresh.token";

  private User buildUser() {
    return User.reconstitute(
        USER_ID,
        EMAIL,
        "juanito",
        "hashed_password",
        "Juan Test",
        "555-0000",
        Role.BUYER,
        true,
        Instant.now(),
        Instant.now());
  }

  // --- happy path ---

  @Test
  void execute_validRefreshToken_returnsNewTokenPair() {
    User user = buildUser();
    when(tokenGenerator.extractUserIdFromRefreshToken(REFRESH_TOKEN)).thenReturn(USER_ID);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(tokenGenerator.generateAccessToken(USER_ID, EMAIL, Role.BUYER))
        .thenReturn(NEW_ACCESS_TOKEN);
    when(tokenGenerator.generateRefreshToken(USER_ID)).thenReturn(NEW_REFRESH_TOKEN);

    LoginUserResult result = useCase.run(new RefreshTokenInput(REFRESH_TOKEN));

    assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
    assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
    assertThat(result.tokenType()).isEqualTo("Bearer");
    assertThat(result.expiresIn()).isEqualTo(86_400L);
  }

  @Test
  void execute_validRefreshToken_rotatesRefreshToken() {
    // token rotation: cada refresh genera un par nuevo, el token anterior queda invalidado
    User user = buildUser();
    when(tokenGenerator.extractUserIdFromRefreshToken(REFRESH_TOKEN)).thenReturn(USER_ID);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(tokenGenerator.generateAccessToken(USER_ID, EMAIL, Role.BUYER))
        .thenReturn(NEW_ACCESS_TOKEN);
    when(tokenGenerator.generateRefreshToken(USER_ID)).thenReturn(NEW_REFRESH_TOKEN);

    LoginUserResult result = useCase.run(new RefreshTokenInput(REFRESH_TOKEN));

    verify(tokenGenerator).generateRefreshToken(USER_ID);
    assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
  }

  @Test
  void execute_validRefreshToken_generatesAccessTokenWithCorrectUserData() {
    User user = buildUser();
    when(tokenGenerator.extractUserIdFromRefreshToken(REFRESH_TOKEN)).thenReturn(USER_ID);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(tokenGenerator.generateAccessToken(USER_ID, EMAIL, Role.BUYER))
        .thenReturn(NEW_ACCESS_TOKEN);
    when(tokenGenerator.generateRefreshToken(USER_ID)).thenReturn(NEW_REFRESH_TOKEN);

    useCase.run(new RefreshTokenInput(REFRESH_TOKEN));

    verify(tokenGenerator).generateAccessToken(USER_ID, EMAIL, Role.BUYER);
  }

  // --- refresh token inválido / expirado ---

  @Test
  void execute_invalidRefreshToken_throwsInvalidRefreshTokenException() {
    when(tokenGenerator.extractUserIdFromRefreshToken(REFRESH_TOKEN))
        .thenThrow(new InvalidRefreshTokenException());

    assertThatThrownBy(() -> useCase.run(new RefreshTokenInput(REFRESH_TOKEN)))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void execute_invalidRefreshToken_neverQueriesUserRepository() {
    when(tokenGenerator.extractUserIdFromRefreshToken(REFRESH_TOKEN))
        .thenThrow(new InvalidRefreshTokenException());

    assertThatThrownBy(() -> useCase.run(new RefreshTokenInput(REFRESH_TOKEN)))
        .isInstanceOf(InvalidRefreshTokenException.class);

    verifyNoInteractions(userRepository);
  }

  // --- usuario eliminado con token válido ---

  @Test
  void execute_validTokenButUserDeleted_throwsInvalidCredentialsException() {
    when(tokenGenerator.extractUserIdFromRefreshToken(REFRESH_TOKEN)).thenReturn(USER_ID);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.run(new RefreshTokenInput(REFRESH_TOKEN)))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void execute_validTokenButUserDeleted_neverGeneratesNewTokens() {
    when(tokenGenerator.extractUserIdFromRefreshToken(REFRESH_TOKEN)).thenReturn(USER_ID);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.run(new RefreshTokenInput(REFRESH_TOKEN)))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(tokenGenerator).extractUserIdFromRefreshToken(REFRESH_TOKEN);
    verify(tokenGenerator, org.mockito.Mockito.never())
        .generateAccessToken(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    verify(tokenGenerator, org.mockito.Mockito.never())
        .generateRefreshToken(org.mockito.ArgumentMatchers.any());
  }
}
