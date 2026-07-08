package controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import controller.user.UserProfileController;
import controller.user.dto.ChangePasswordRequest;
import controller.user.dto.UpdateProfileRequest;
import domain.user.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import user.ChangePasswordUseCase;
import user.GetUserProfileUseCase;
import user.UpdateUserProfileUseCase;
import user.input.ChangePasswordInput;
import user.input.GetUserProfileInput;
import user.input.UpdateUserProfileInput;
import user.output.ChangePasswordResult;
import user.output.GetUserProfileResult;
import user.output.UpdateUserProfileResult;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

  @Mock private GetUserProfileUseCase getUserProfileUseCase;
  @Mock private UpdateUserProfileUseCase updateUserProfileUseCase;
  @Mock private ChangePasswordUseCase changePasswordUseCase;

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    UserProfileController controller =
        new UserProfileController(
            getUserProfileUseCase, updateUserProfileUseCase, changePasswordUseCase);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setValidator(
                new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean())
            .build();

    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(userId.toString(), "password", "ROLE_BUYER"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getProfile_withAuthenticatedUser_returnsOk() throws Exception {
    GetUserProfileResult result =
        new GetUserProfileResult(
            userId,
            "test@example.com",
            "testuser",
            "Test User",
            "555-1234",
            Role.BUYER,
            Instant.now());

    when(getUserProfileUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(get("/users/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(jsonPath("$.role").value("BUYER"));

    ArgumentCaptor<GetUserProfileInput> captor = ArgumentCaptor.forClass(GetUserProfileInput.class);
    verify(getUserProfileUseCase).run(captor.capture());
    assertThat(captor.getValue().userId()).isEqualTo(userId);
  }

  @Test
  void updateProfile_withValidRequest_returnsOk() throws Exception {
    UpdateProfileRequest request =
        new UpdateProfileRequest("new@example.com", "newuser", "New Name", "555-9999");

    UpdateUserProfileResult result =
        new UpdateUserProfileResult(userId, "new@example.com", "newuser", "New Name", "555-9999");

    when(updateUserProfileUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(
            patch("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.email").value("new@example.com"))
        .andExpect(jsonPath("$.username").value("newuser"));

    ArgumentCaptor<UpdateUserProfileInput> captor =
        ArgumentCaptor.forClass(UpdateUserProfileInput.class);
    verify(updateUserProfileUseCase).run(captor.capture());
    assertThat(captor.getValue().userId()).isEqualTo(userId);
    assertThat(captor.getValue().email()).isEqualTo("new@example.com");
    assertThat(captor.getValue().username()).isEqualTo("newuser");
    assertThat(captor.getValue().fullName()).isEqualTo("New Name");
    assertThat(captor.getValue().phone()).isEqualTo("555-9999");
  }

  @Test
  void updateProfile_withInvalidEmail_returnsBadRequest() throws Exception {
    UpdateProfileRequest request = new UpdateProfileRequest("not-an-email", null, null, null);

    mockMvc
        .perform(
            patch("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void changePassword_withValidRequest_returnsOk() throws Exception {
    ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "newPassword");

    ChangePasswordResult result = new ChangePasswordResult(userId);

    when(changePasswordUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(
            post("/users/me/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()));

    ArgumentCaptor<ChangePasswordInput> captor = ArgumentCaptor.forClass(ChangePasswordInput.class);
    verify(changePasswordUseCase).run(captor.capture());
    assertThat(captor.getValue().userId()).isEqualTo(userId);
    assertThat(captor.getValue().currentPassword()).isEqualTo("oldPassword");
    assertThat(captor.getValue().newPassword()).isEqualTo("newPassword");
  }

  @Test
  void changePassword_withBlankNewPassword_returnsBadRequest() throws Exception {
    ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "");

    mockMvc
        .perform(
            post("/users/me/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }
}
