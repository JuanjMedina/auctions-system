package user;

import domain.user.User;
import domain.user.UserExceptions;
import domain.user.UserPasswordEncoder;
import domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;
import user.input.ChangePasswordInput;
import user.output.ChangePasswordResult;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase implements UseCase<ChangePasswordInput, ChangePasswordResult> {

  private final UserRepository userRepository;
  private final UserPasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public ChangePasswordResult run(ChangePasswordInput input) {
    return UseCase.super.run(input);
  }

  @Override
  public ChangePasswordResult execute(ChangePasswordInput input) {
    User user = userRepository.getById(input.userId());

    if (!passwordEncoder.matches(input.currentPassword(), user.getPasswordHash())) {
      throw new UserExceptions.InvalidCurrentPasswordException();
    }

    user.changePassword(passwordEncoder.encode(input.newPassword()));
    User saved = userRepository.save(user);

    return new ChangePasswordResult(saved.getId());
  }

  @Override
  public String errorMessage() {
    return "Error al cambiar la contraseña";
  }
}
