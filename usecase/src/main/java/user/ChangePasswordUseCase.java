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
  public ChangePasswordResult execute(ChangePasswordInput input) {
    User user =
        userRepository
            .findById(input.userId())
            .orElseThrow(() -> new UserExceptions.UserNotFoundException(input.userId()));

    if (!passwordEncoder.matches(input.currentPassword(), user.getPasswordHash())) {
      throw new UserExceptions.InvalidCurrentPasswordException();
    }

    user.changePassword(passwordEncoder.encode(input.newPassword()));
    User saved = userRepository.save(user);

    return new ChangePasswordResult(saved.getId());
  }

  @Override
  public ChangePasswordResult failed(Exception exception) {
    if (exception instanceof UserExceptions.UserNotFoundException e) throw e;
    if (exception instanceof UserExceptions.InvalidCurrentPasswordException e) throw e;
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al cambiar la contraseña", exception);
  }
}
