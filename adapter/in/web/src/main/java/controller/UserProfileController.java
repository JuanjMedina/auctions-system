package controller;

import controller.dto.request.ChangePasswordRequest;
import controller.dto.request.UpdateProfileRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import security.SecurityUtils;
import user.ChangePasswordUseCase;
import user.GetUserProfileUseCase;
import user.UpdateUserProfileUseCase;
import user.input.ChangePasswordInput;
import user.input.GetUserProfileInput;
import user.input.UpdateUserProfileInput;
import user.output.ChangePasswordResult;
import user.output.GetUserProfileResult;
import user.output.UpdateUserProfileResult;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserProfileController {

  private final GetUserProfileUseCase getUserProfileUseCase;
  private final UpdateUserProfileUseCase updateUserProfileUseCase;
  private final ChangePasswordUseCase changePasswordUseCase;

  @GetMapping
  public ResponseEntity<GetUserProfileResult> getProfile() {
    UUID userId = SecurityUtils.currentUserId();
    return ResponseEntity.ok(getUserProfileUseCase.run(new GetUserProfileInput(userId)));
  }

  @PatchMapping
  public ResponseEntity<UpdateUserProfileResult> updateProfile(
      @Valid @RequestBody UpdateProfileRequest request) {
    UUID userId = SecurityUtils.currentUserId();
    UpdateUserProfileInput input =
        new UpdateUserProfileInput(
            userId, request.email(), request.username(), request.fullName(), request.phone());
    return ResponseEntity.ok(updateUserProfileUseCase.run(input));
  }

  @PostMapping("/change-password")
  public ResponseEntity<ChangePasswordResult> changePassword(
      @Valid @RequestBody ChangePasswordRequest request) {
    UUID userId = SecurityUtils.currentUserId();
    ChangePasswordInput input =
        new ChangePasswordInput(userId, request.currentPassword(), request.newPassword());
    return ResponseEntity.ok(changePasswordUseCase.run(input));
  }
}
