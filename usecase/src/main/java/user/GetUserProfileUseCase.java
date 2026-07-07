package user;

import domain.user.User;
import domain.user.UserExceptions;
import domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;
import user.input.GetUserProfileInput;
import user.output.GetUserProfileResult;

@Service
@RequiredArgsConstructor
public class GetUserProfileUseCase implements UseCase<GetUserProfileInput, GetUserProfileResult> {

  private final UserRepository userRepository;

  @Override
  public GetUserProfileResult execute(GetUserProfileInput input) {
    User user =
        userRepository
            .findById(input.userId())
            .orElseThrow(() -> new UserExceptions.UserNotFoundException(input.userId()));

    return new GetUserProfileResult(
        user.getId(),
        user.getEmail(),
        user.getUsername(),
        user.getFullName(),
        user.getPhone(),
        user.getRole(),
        user.getCreatedAt());
  }

  @Override
  public GetUserProfileResult failed(Exception exception) {
    if (exception instanceof UserExceptions.UserNotFoundException e) throw e;
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al obtener el perfil", exception);
  }
}
