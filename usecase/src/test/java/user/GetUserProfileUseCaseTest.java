package user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import domain.user.Role;
import domain.user.User;
import domain.user.UserExceptions.UserNotFoundException;
import domain.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import user.input.GetUserProfileInput;
import user.output.GetUserProfileResult;

@ExtendWith(MockitoExtension.class)
class GetUserProfileUseCaseTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private GetUserProfileUseCase useCase;

  // --- fixtures ---
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String EMAIL = "juan@test.com";
  private static final String USERNAME = "juanito";
  private static final Instant CREATED_AT = Instant.now();

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
        CREATED_AT,
        CREATED_AT);
  }

  // --- happy path ---

  @Test
  void execute_existingUser_returnsProfileWithCorrectData() {
    // arrange
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));

    // act
    GetUserProfileResult result = useCase.run(new GetUserProfileInput(USER_ID));

    // assert
    assertThat(result.id()).isEqualTo(USER_ID);
    assertThat(result.email()).isEqualTo(EMAIL);
    assertThat(result.username()).isEqualTo(USERNAME);
    assertThat(result.fullName()).isEqualTo("Juan Test");
    assertThat(result.phone()).isEqualTo("555-0000");
    assertThat(result.role()).isEqualTo(Role.BUYER);
    assertThat(result.createdAt()).isEqualTo(CREATED_AT);
  }

  // --- usuario no encontrado ---

  @Test
  void execute_userNotFound_throwsUserNotFoundException() {
    // arrange
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(new GetUserProfileInput(USER_ID)))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void execute_userNotFound_exceptionContainsUserId() {
    // arrange
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(new GetUserProfileInput(USER_ID)))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining(USER_ID.toString());
  }
}
