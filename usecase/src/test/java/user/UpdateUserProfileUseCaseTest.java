package user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.user.Role;
import domain.user.User;
import domain.user.UserExceptions.EmailAlreadyTakenException;
import domain.user.UserExceptions.UserNotFoundException;
import domain.user.UserExceptions.UsernameAlreadyTakenException;
import domain.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import user.input.UpdateUserProfileInput;
import user.output.UpdateUserProfileResult;

@ExtendWith(MockitoExtension.class)
class UpdateUserProfileUseCaseTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UpdateUserProfileUseCase useCase;

  // --- fixtures ---
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String EMAIL = "juan@test.com";
  private static final String USERNAME = "juanito";

  private User buildUser() {
    return User.reconstitute(
        USER_ID,
        EMAIL,
        USERNAME,
        "$2a$12$hashedpassword",
        "Juan Test",
        "555-0000",
        Role.BUYER,
        true,
        Instant.now(),
        Instant.now());
  }

  private void setupSaveAnswer() {
    when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  // --- happy path: sin cambios de email/username ---

  @Test
  void execute_onlyFullNameAndPhone_updatesProfileFields() {
    // arrange
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
    setupSaveAnswer();

    // act
    UpdateUserProfileResult result =
        useCase.run(new UpdateUserProfileInput(USER_ID, null, null, "Juan Nuevo", "555-9999"));

    // assert
    assertThat(result.id()).isEqualTo(USER_ID);
    assertThat(result.fullName()).isEqualTo("Juan Nuevo");
    assertThat(result.phone()).isEqualTo("555-9999");
    assertThat(result.email()).isEqualTo(EMAIL);
    assertThat(result.username()).isEqualTo(USERNAME);
  }

  @Test
  void execute_onlyFullNameAndPhone_neverChecksEmailOrUsernameUniqueness() {
    // arrange
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
    setupSaveAnswer();

    // act
    useCase.run(new UpdateUserProfileInput(USER_ID, null, null, "Juan Nuevo", "555-9999"));

    // assert
    verify(userRepository, never()).existsByEmail(any());
    verify(userRepository, never()).existsByUsername(any());
  }

  // --- cambio de email exitoso ---

  @Test
  void execute_newAvailableEmail_updatesEmail() {
    // arrange
    String newEmail = "nuevo@test.com";
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
    when(userRepository.existsByEmail(newEmail)).thenReturn(false);
    setupSaveAnswer();

    // act
    UpdateUserProfileResult result =
        useCase.run(new UpdateUserProfileInput(USER_ID, newEmail, null, null, null));

    // assert
    assertThat(result.email()).isEqualTo(newEmail);
  }

  @Test
  void execute_sameEmailAsCurrent_doesNotCheckUniqueness() {
    // arrange
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
    setupSaveAnswer();

    // act
    UpdateUserProfileResult result =
        useCase.run(new UpdateUserProfileInput(USER_ID, EMAIL, null, null, null));

    // assert
    assertThat(result.email()).isEqualTo(EMAIL);
    verify(userRepository, never()).existsByEmail(any());
  }

  // --- email ya en uso ---

  @Test
  void execute_emailAlreadyTaken_throwsEmailAlreadyTakenException() {
    // arrange
    String takenEmail = "otro@test.com";
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
    when(userRepository.existsByEmail(takenEmail)).thenReturn(true);

    // act & assert
    assertThatThrownBy(
            () -> useCase.run(new UpdateUserProfileInput(USER_ID, takenEmail, null, null, null)))
        .isInstanceOf(EmailAlreadyTakenException.class);
  }

  @Test
  void execute_emailAlreadyTaken_neverSaves() {
    // arrange
    String takenEmail = "otro@test.com";
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
    when(userRepository.existsByEmail(takenEmail)).thenReturn(true);

    // act & assert
    assertThatThrownBy(
            () -> useCase.run(new UpdateUserProfileInput(USER_ID, takenEmail, null, null, null)))
        .isInstanceOf(EmailAlreadyTakenException.class);

    verify(userRepository, never()).save(any());
  }

  // --- cambio de username exitoso ---

  @Test
  void execute_newAvailableUsername_updatesUsername() {
    // arrange
    String newUsername = "nuevoUsuario";
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
    when(userRepository.existsByUsername(newUsername)).thenReturn(false);
    setupSaveAnswer();

    // act
    UpdateUserProfileResult result =
        useCase.run(new UpdateUserProfileInput(USER_ID, null, newUsername, null, null));

    // assert
    assertThat(result.username()).isEqualTo(newUsername);
  }

  // --- username ya en uso ---

  @Test
  void execute_usernameAlreadyTaken_throwsUsernameAlreadyTakenException() {
    // arrange
    String takenUsername = "otroUsuario";
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
    when(userRepository.existsByUsername(takenUsername)).thenReturn(true);

    // act & assert
    assertThatThrownBy(
            () -> useCase.run(new UpdateUserProfileInput(USER_ID, null, takenUsername, null, null)))
        .isInstanceOf(UsernameAlreadyTakenException.class);
  }

  @Test
  void execute_usernameAlreadyTaken_neverSaves() {
    // arrange
    String takenUsername = "otroUsuario";
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
    when(userRepository.existsByUsername(takenUsername)).thenReturn(true);

    // act & assert
    assertThatThrownBy(
            () -> useCase.run(new UpdateUserProfileInput(USER_ID, null, takenUsername, null, null)))
        .isInstanceOf(UsernameAlreadyTakenException.class);

    verify(userRepository, never()).save(any());
  }

  // --- usuario no encontrado ---

  @Test
  void execute_userNotFound_throwsUserNotFoundException() {
    // arrange
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(
            () -> useCase.run(new UpdateUserProfileInput(USER_ID, null, null, "Nuevo", null)))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void execute_userNotFound_neverSaves() {
    // arrange
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(
            () -> useCase.run(new UpdateUserProfileInput(USER_ID, null, null, "Nuevo", null)))
        .isInstanceOf(UserNotFoundException.class);

    verify(userRepository, never()).save(any());
  }
}
