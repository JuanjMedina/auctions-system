package user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.user.Role;
import domain.user.User;
import domain.user.UserExceptions.InvalidCurrentPasswordException;
import domain.user.UserExceptions.UserNotFoundException;
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
import user.input.ChangePasswordInput;
import user.output.ChangePasswordResult;

@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseTest {

  @Mock private UserRepository userRepository;
  @Mock private UserPasswordEncoder passwordEncoder;

  @InjectMocks private ChangePasswordUseCase useCase;

  // --- fixtures ---
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String CURRENT_PASSWORD = "oldPassword123";
  private static final String NEW_PASSWORD = "newPassword456";
  private static final String CURRENT_HASH = "$2a$12$currenthash";
  private static final String NEW_HASH = "$2a$12$newhash";

  private User buildUser() {
    return User.reconstitute(
        USER_ID,
        "juan@test.com",
        "juanito",
        CURRENT_HASH,
        "Juan Test",
        "555-0000",
        Role.BUYER,
        true,
        Instant.now(),
        Instant.now());
  }

  private ChangePasswordInput validInput() {
    return new ChangePasswordInput(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);
  }

  // --- happy path ---

  @Test
  void execute_validCurrentPassword_returnsResultWithUserId() {
    // arrange
    User user = buildUser();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH)).thenReturn(true);
    when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);
    when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    ChangePasswordResult result = useCase.run(validInput());

    // assert
    assertThat(result.id()).isEqualTo(USER_ID);
  }

  @Test
  void execute_validCurrentPassword_hashesNewPasswordBeforeSaving() {
    // arrange
    User user = buildUser();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH)).thenReturn(true);
    when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);
    when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    useCase.run(validInput());

    // assert
    verify(passwordEncoder).encode(NEW_PASSWORD);
    verify(userRepository).save(argThat(u -> NEW_HASH.equals(u.getPasswordHash())));
  }

  // --- usuario no encontrado ---

  @Test
  void execute_userNotFound_throwsUserNotFoundException() {
    // arrange
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput())).isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void execute_userNotFound_neverChecksPasswordOrSaves() {
    // arrange
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput())).isInstanceOf(UserNotFoundException.class);

    verify(passwordEncoder, never()).matches(any(), any());
    verify(userRepository, never()).save(any());
  }

  // --- contraseña actual incorrecta ---

  @Test
  void execute_wrongCurrentPassword_throwsInvalidCurrentPasswordException() {
    // arrange
    User user = buildUser();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH)).thenReturn(false);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(InvalidCurrentPasswordException.class);
  }

  @Test
  void execute_wrongCurrentPassword_neverEncodesOrSavesNewPassword() {
    // arrange
    User user = buildUser();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH)).thenReturn(false);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(InvalidCurrentPasswordException.class);

    verify(passwordEncoder, never()).encode(any());
    verify(userRepository, never()).save(any());
  }

  // --- helper para capturar argumentos con assertj ---
  private static <T> T argThat(java.util.function.Predicate<T> predicate) {
    return org.mockito.ArgumentMatchers.argThat(predicate::test);
  }
}
