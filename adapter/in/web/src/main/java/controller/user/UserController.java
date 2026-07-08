package controller.user;

import controller.user.dto.LoginRequest;
import controller.user.dto.RefreshTokenRequest;
import controller.user.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import user.LoginUserUseCase;
import user.RefreshTokenUseCase;
import user.RegisterUserUseCase;
import user.input.LoginUserInput;
import user.input.RefreshTokenInput;
import user.input.RegisterUserInput;
import user.output.LoginUserResult;
import user.output.RegisterUserResult;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

  private final RegisterUserUseCase registerUserUseCase;
  private final LoginUserUseCase loginUserUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;

  @PostMapping("/register")
  public ResponseEntity<RegisterUserResult> register(@Valid @RequestBody RegisterRequest request) {
    RegisterUserInput input =
        new RegisterUserInput(
            request.email(),
            request.username(),
            request.rawPassword(),
            request.fullName(),
            request.phone(),
            request.role());
    return ResponseEntity.status(HttpStatus.CREATED).body(registerUserUseCase.run(input));
  }

  @PostMapping("/login")
  public ResponseEntity<LoginUserResult> login(@Valid @RequestBody LoginRequest request) {
    LoginUserInput input = new LoginUserInput(null, request.email(), request.password());
    return ResponseEntity.ok(loginUserUseCase.run(input));
  }

  @PostMapping("/refresh")
  public ResponseEntity<LoginUserResult> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(
        refreshTokenUseCase.run(new RefreshTokenInput(request.refreshToken())));
  }
}
