package adapter.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.user.Role;
import domain.user.User;
import entity.user.UserEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.user.SpringDataUserRepository;

@ExtendWith(MockitoExtension.class)
class UserJpaAdapterTest {

  @Mock private SpringDataUserRepository springDataRepo;

  private UserJpaAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new UserJpaAdapter(springDataRepo);
  }

  private User buildUser(UUID id, String email, String username) {
    return User.reconstitute(
        id,
        email,
        username,
        "hashed-password",
        "John Doe",
        "+1234567890",
        Role.BUYER,
        true,
        Instant.now(),
        Instant.now());
  }

  private UserEntity buildEntity(UUID id, String email, String username) {
    return UserEntity.builder()
        .id(id)
        .email(email)
        .username(username)
        .passwordHash("hashed-password")
        .fullName("John Doe")
        .phone("+1234567890")
        .role(Role.BUYER)
        .isActive(true)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  // --- save ---

  @Test
  void save_validUser_delegatesToSaveAndMapsResult() {
    // arrange
    UUID id = UUID.randomUUID();
    User user = buildUser(id, "john@test.com", "johndoe");
    UserEntity savedEntity = buildEntity(id, "john@test.com", "johndoe");

    when(springDataRepo.save(any(UserEntity.class))).thenReturn(savedEntity);

    // act
    User result = adapter.save(user);

    // assert
    ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
    verify(springDataRepo).save(captor.capture());
    UserEntity captured = captor.getValue();

    assertThat(captured.getId()).isEqualTo(id);
    assertThat(captured.getEmail()).isEqualTo("john@test.com");
    assertThat(captured.getUsername()).isEqualTo("johndoe");
    assertThat(captured.getPasswordHash()).isEqualTo("hashed-password");
    assertThat(captured.getFullName()).isEqualTo("John Doe");
    assertThat(captured.getPhone()).isEqualTo("+1234567890");
    assertThat(captured.getRole()).isEqualTo(Role.BUYER);
    assertThat(captured.isActive()).isTrue();

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getEmail()).isEqualTo("john@test.com");
  }

  // --- findById ---

  @Test
  void findById_existingUser_returnsMappedDomain() {
    // arrange
    UUID id = UUID.randomUUID();
    UserEntity entity = buildEntity(id, "john@test.com", "johndoe");
    when(springDataRepo.findById(id)).thenReturn(Optional.of(entity));

    // act
    Optional<User> result = adapter.findById(id);

    // assert
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(id);
    assertThat(result.get().getRole()).isEqualTo(Role.BUYER);
  }

  @Test
  void findById_missingUser_returnsEmptyOptional() {
    // arrange
    UUID id = UUID.randomUUID();
    when(springDataRepo.findById(id)).thenReturn(Optional.empty());

    // act
    Optional<User> result = adapter.findById(id);

    // assert
    assertThat(result).isEmpty();
  }

  // --- findByEmail ---

  @Test
  void findByEmail_existingEmail_returnsMappedDomain() {
    // arrange
    UserEntity entity = buildEntity(UUID.randomUUID(), "john@test.com", "johndoe");
    when(springDataRepo.findByEmail("john@test.com")).thenReturn(Optional.of(entity));

    // act
    Optional<User> result = adapter.findByEmail("john@test.com");

    // assert
    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo("john@test.com");
  }

  @Test
  void findByEmail_missingEmail_returnsEmptyOptional() {
    // arrange
    when(springDataRepo.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

    // act
    Optional<User> result = adapter.findByEmail("unknown@test.com");

    // assert
    assertThat(result).isEmpty();
  }

  // --- findByUsername ---

  @Test
  void findByUsername_existingUsername_returnsMappedDomain() {
    // arrange
    UserEntity entity = buildEntity(UUID.randomUUID(), "john@test.com", "johndoe");
    when(springDataRepo.findByUsername("johndoe")).thenReturn(Optional.of(entity));

    // act
    Optional<User> result = adapter.findByUsername("johndoe");

    // assert
    assertThat(result).isPresent();
    assertThat(result.get().getUsername()).isEqualTo("johndoe");
  }

  @Test
  void findByUsername_missingUsername_returnsEmptyOptional() {
    // arrange
    when(springDataRepo.findByUsername("unknown")).thenReturn(Optional.empty());

    // act
    Optional<User> result = adapter.findByUsername("unknown");

    // assert
    assertThat(result).isEmpty();
  }

  // --- existsByEmail ---

  @Test
  void existsByEmail_emailExists_returnsTrue() {
    // arrange
    when(springDataRepo.existsByEmail("john@test.com")).thenReturn(true);

    // act
    boolean result = adapter.existsByEmail("john@test.com");

    // assert
    assertThat(result).isTrue();
  }

  @Test
  void existsByEmail_emailMissing_returnsFalse() {
    // arrange
    when(springDataRepo.existsByEmail("unknown@test.com")).thenReturn(false);

    // act
    boolean result = adapter.existsByEmail("unknown@test.com");

    // assert
    assertThat(result).isFalse();
  }

  // --- existsByUsername ---

  @Test
  void existsByUsername_usernameExists_returnsTrue() {
    // arrange
    when(springDataRepo.existsByUsername("johndoe")).thenReturn(true);

    // act
    boolean result = adapter.existsByUsername("johndoe");

    // assert
    assertThat(result).isTrue();
  }

  @Test
  void existsByUsername_usernameMissing_returnsFalse() {
    // arrange
    when(springDataRepo.existsByUsername("unknown")).thenReturn(false);

    // act
    boolean result = adapter.existsByUsername("unknown");

    // assert
    assertThat(result).isFalse();
  }
}
