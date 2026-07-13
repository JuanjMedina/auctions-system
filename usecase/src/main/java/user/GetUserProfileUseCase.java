package user;

import domain.user.User;
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
    User user = userRepository.getById(input.userId());

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
  public String errorMessage() {
    return "Error al obtener el perfil";
  }
}
