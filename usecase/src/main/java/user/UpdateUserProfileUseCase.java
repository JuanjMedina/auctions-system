package user;

import domain.user.User;
import domain.user.UserExceptions;
import domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;
import user.input.UpdateUserProfileInput;
import user.output.UpdateUserProfileResult;

@Service
@RequiredArgsConstructor
public class UpdateUserProfileUseCase
    implements UseCase<UpdateUserProfileInput, UpdateUserProfileResult> {

  private final UserRepository userRepository;

  @Override
  @Transactional
  public UpdateUserProfileResult execute(UpdateUserProfileInput input) {
    User user =
        userRepository
            .findById(input.userId())
            .orElseThrow(() -> new UserExceptions.UserNotFoundException(input.userId()));

    if (input.email() != null && !input.email().equals(user.getEmail())) {
      if (userRepository.existsByEmail(input.email())) {
        throw new UserExceptions.EmailAlreadyTakenException(input.email());
      }
      user.changeEmail(input.email());
    }

    if (input.username() != null && !input.username().equals(user.getUsername())) {
      if (userRepository.existsByUsername(input.username())) {
        throw new UserExceptions.UsernameAlreadyTakenException(input.username());
      }
      user.changeUsername(input.username());
    }

    if (input.fullName() != null || input.phone() != null) {
      String fullName = input.fullName() != null ? input.fullName() : user.getFullName();
      String phone = input.phone() != null ? input.phone() : user.getPhone();
      user.updateProfile(fullName, phone);
    }

    User saved = userRepository.save(user);

    return new UpdateUserProfileResult(
        saved.getId(),
        saved.getEmail(),
        saved.getUsername(),
        saved.getFullName(),
        saved.getPhone());
  }

  @Override
  public UpdateUserProfileResult failed(Exception exception) {
    if (exception instanceof UserExceptions.UserNotFoundException e) throw e;
    if (exception instanceof UserExceptions.EmailAlreadyTakenException e) throw e;
    if (exception instanceof UserExceptions.UsernameAlreadyTakenException e) throw e;
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al actualizar el perfil", exception);
  }
}
