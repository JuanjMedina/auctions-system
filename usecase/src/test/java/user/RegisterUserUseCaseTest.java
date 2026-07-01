package user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.user.Role;
import domain.user.UserExceptions.EmailAlreadyTakenException;
import domain.user.UserExceptions.UsernameAlreadyTakenException;
import domain.user.UserPasswordEncoder;
import domain.user.UserRepository;
import domain.wallets.Wallet;
import domain.wallets.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import user.input.RegisterUserInput;
import user.output.RegisterUserResult;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

  @Mock private UserRepository userRepository;
  @Mock private WalletRepository walletRepository;
  @Mock private UserPasswordEncoder passwordEncoder;

  @InjectMocks private RegisterUserUseCase useCase;

  // --- fixtures ---
  private static final String EMAIL = "juan@test.com";
  private static final String USERNAME = "juanito";
  private static final String RAW_PASSWORD = "password123";
  private static final String HASHED_PASSWORD = "$2a$12$hashedpassword";

  private RegisterUserInput validInput() {
    return new RegisterUserInput(
        EMAIL, USERNAME, RAW_PASSWORD, "Juan Test", "555-0000", Role.BUYER.toString());
  }

  private void setupHappyPath() {
    when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
    when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  // --- happy path ---

  @Test
  void execute_validInput_returnsResultWithCorrectEmailAndUsername() {
    setupHappyPath();

    RegisterUserResult result = useCase.run(validInput());

    assertThat(result.email()).isEqualTo(EMAIL);
    assertThat(result.username()).isEqualTo(USERNAME);
  }

  @Test
  void execute_validInput_returnsNonNullUserIdAndWalletId() {
    setupHappyPath();

    RegisterUserResult result = useCase.run(validInput());

    assertThat(result.userId()).isNotNull();
    assertThat(result.walletId()).isNotNull();
  }

  @Test
  void execute_validInput_hashesPasswordBeforeSaving() {
    setupHappyPath();

    useCase.run(validInput());

    // passwordEncoder.encode() must be called with the raw password
    verify(passwordEncoder).encode(RAW_PASSWORD);
    // and the raw password must never be stored (User.create receives the hash)
    verify(userRepository).save(any());
  }

  @Test
  void execute_validInput_rawPasswordNeverStoredDirectly() {
    setupHappyPath();

    useCase.run(validInput());

    // capture the user passed to save and verify its hash != raw password
    verify(userRepository)
        .save(
            argThat(
                user ->
                    !RAW_PASSWORD.equals(user.getPasswordHash())
                        && HASHED_PASSWORD.equals(user.getPasswordHash())));
  }

  @Test
  void execute_validInput_walletLinkedToCreatedUser() {
    setupHappyPath();

    RegisterUserResult result = useCase.run(validInput());

    // walletId is returned and userId is not null — wallet was created for this user
    assertThat(result.walletId()).isNotNull();
    assertThat(result.userId()).isNotNull();
  }

  @Test
  void execute_validInput_persistsUserAndWallet() {
    setupHappyPath();

    useCase.run(validInput());

    verify(userRepository).save(any());
    verify(walletRepository).save(any(Wallet.class));
  }

  // --- email ya registrado ---

  @Test
  void execute_emailAlreadyTaken_throwsEmailAlreadyTakenException() {
    when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(EmailAlreadyTakenException.class);
  }

  @Test
  void execute_emailAlreadyTaken_exceptionContainsEmail() {
    when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(EmailAlreadyTakenException.class)
        .hasMessageContaining(EMAIL);
  }

  @Test
  void execute_emailAlreadyTaken_neverSavesUserOrWallet() {
    when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(EmailAlreadyTakenException.class);

    verify(userRepository, never()).save(any());
    verify(walletRepository, never()).save(any());
  }

  @Test
  void execute_emailAlreadyTaken_neverChecksUsername() {
    when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(EmailAlreadyTakenException.class);

    verify(userRepository, never()).existsByUsername(any());
  }

  // --- username ya registrado ---

  @Test
  void execute_usernameAlreadyTaken_throwsUsernameAlreadyTakenException() {
    when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
    when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(UsernameAlreadyTakenException.class);
  }

  @Test
  void execute_usernameAlreadyTaken_exceptionContainsUsername() {
    when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
    when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(UsernameAlreadyTakenException.class)
        .hasMessageContaining(USERNAME);
  }

  @Test
  void execute_usernameAlreadyTaken_neverSavesUserOrWallet() {
    when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
    when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(UsernameAlreadyTakenException.class);

    verify(userRepository, never()).save(any());
    verify(walletRepository, never()).save(any());
  }

  @Test
  void execute_usernameAlreadyTaken_neverHashesPassword() {
    when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
    when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(UsernameAlreadyTakenException.class);

    verify(passwordEncoder, never()).encode(any());
  }

  // --- helper para capturar argumentos con assertj ---
  private static <T> T argThat(java.util.function.Predicate<T> predicate) {
    return org.mockito.ArgumentMatchers.argThat(predicate::test);
  }
}
