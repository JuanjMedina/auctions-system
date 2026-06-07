package user;

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
            input.role());

    User savedUser = userRepository.save(user);

    Wallet wallet = Wallet.create(savedUser.getId());
    Wallet savedWallet = walletRepository.save(wallet);

    return new RegisterUserResult(
        savedUser.getId(), savedUser.getEmail(), savedUser.getUsername(), savedWallet.getId());
  }

  @Override
  public RegisterUserResult failed(RuntimeException exception) {
    throw exception;
  }
}
