package user;

import domain.user.Role;
import domain.user.User;
import domain.user.UserExceptions;
import domain.user.UserPasswordEncoder;
import domain.user.UserRepository;
import domain.wallets.Wallet;
import domain.wallets.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;
import user.input.RegisterUserInput;
import user.output.RegisterUserResult;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase implements UseCase<RegisterUserInput, RegisterUserResult> {

  private final UserRepository userRepository;
  private final WalletRepository walletRepository;
  private final UserPasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public RegisterUserResult execute(RegisterUserInput input) {
    if (userRepository.existsByEmail(input.email())) {
      throw new UserExceptions.EmailAlreadyTakenException(input.email());
    }
    if (userRepository.existsByUsername(input.username())) {
      throw new UserExceptions.UsernameAlreadyTakenException(input.username());
    }

    String passwordHash = passwordEncoder.encode(input.rawPassword());
    User user =
        User.create(
            input.email(),
            input.username(),
            passwordHash,
            input.fullName(),
            input.phone(),
            resolveRole(input.role()));

    User savedUser = userRepository.save(user);

    Wallet wallet = Wallet.create(savedUser.getId());
    Wallet savedWallet = walletRepository.save(wallet);

    return new RegisterUserResult(
        savedUser.getId(), savedUser.getEmail(), savedUser.getUsername(), savedWallet.getId());
  }

  private Role resolveRole(String role) {
    if (role == null) return Role.SELLER;
    try {
      return Role.valueOf(role.toUpperCase());
    } catch (IllegalArgumentException e) {
      return Role.SELLER;
    }
  }

  @Override
  public RegisterUserResult failed(Exception exception) {
    if (exception instanceof UserExceptions.EmailAlreadyTakenException e) throw e;
    if (exception instanceof UserExceptions.UsernameAlreadyTakenException e) throw e;
    throw new RuntimeException("Error inesperado en registro", exception);
  }
}
