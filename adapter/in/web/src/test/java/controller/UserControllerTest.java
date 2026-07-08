package controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import controller.dto.request.LoginRequest;
import controller.dto.request.RefreshTokenRequest;
import controller.dto.request.RegisterRequest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import user.LoginUserUseCase;
import user.RefreshTokenUseCase;
import user.RegisterUserUseCase;
import user.input.LoginUserInput;
import user.input.RefreshTokenInput;
import user.input.RegisterUserInput;
import user.output.LoginUserResult;
import user.output.RegisterUserResult;

// Standalone: registra solo UserController con mocks Mockito, sin levantar
// ApplicationContext de Spring.
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock private RegisterUserUseCase registerUserUseCase;
  @Mock private LoginUserUseCase loginUserUseCase;
  @Mock private RefreshTokenUseCase refreshTokenUseCase;

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @BeforeEach
  void setUp() {
    UserController controller =
        new UserController(registerUserUseCase, loginUserUseCase, refreshTokenUseCase);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setValidator(
                new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean())
            .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void register_withValidRequest_returnsCreated() throws Exception {
    RegisterRequest request =
        new RegisterRequest(
            "test@example.com", "testuser", "raw-password", "Test User", "555-1234", "BUYER");

    RegisterUserResult result =
        new RegisterUserResult(
            UUID.randomUUID(), "test@example.com", "testuser", UUID.randomUUID());

    when(registerUserUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(result.userId().toString()))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(jsonPath("$.walletId").value(result.walletId().toString()));

    ArgumentCaptor<RegisterUserInput> captor = ArgumentCaptor.forClass(RegisterUserInput.class);
    verify(registerUserUseCase).run(captor.capture());
    assertThat(captor.getValue().email()).isEqualTo("test@example.com");
    assertThat(captor.getValue().username()).isEqualTo("testuser");
    assertThat(captor.getValue().rawPassword()).isEqualTo("raw-password");
    assertThat(captor.getValue().fullName()).isEqualTo("Test User");
    assertThat(captor.getValue().phone()).isEqualTo("555-1234");
    assertThat(captor.getValue().role()).isEqualTo("BUYER");
  }

  @Test
  void register_withBlankEmail_returnsBadRequest() throws Exception {
    RegisterRequest request =
        new RegisterRequest("", "testuser", "raw-password", "Test User", "555-1234", "BUYER");

    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_withValidCredentials_returnsOk() throws Exception {
    LoginRequest request = new LoginRequest("test@example.com", "password123");

    LoginUserResult result =
        new LoginUserResult("access-token", "refresh-token", "Bearer", 86_400L);

    when(loginUserUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access-token"))
        .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(86_400));

    ArgumentCaptor<LoginUserInput> captor = ArgumentCaptor.forClass(LoginUserInput.class);
    verify(loginUserUseCase).run(captor.capture());
    assertThat(captor.getValue().email()).isEqualTo("test@example.com");
    assertThat(captor.getValue().password()).isEqualTo("password123");
    assertThat(captor.getValue().username()).isNull();
  }

  @Test
  void login_withBlankPassword_returnsBadRequest() throws Exception {
    LoginRequest request = new LoginRequest("test@example.com", "");

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void refresh_withValidToken_returnsOk() throws Exception {
    RefreshTokenRequest request = new RefreshTokenRequest("some-refresh-token");

    LoginUserResult result =
        new LoginUserResult("new-access-token", "new-refresh-token", "Bearer", 86_400L);

    when(refreshTokenUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("new-access-token"))
        .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

    ArgumentCaptor<RefreshTokenInput> captor = ArgumentCaptor.forClass(RefreshTokenInput.class);
    verify(refreshTokenUseCase).run(captor.capture());
    assertThat(captor.getValue().refreshToken()).isEqualTo("some-refresh-token");
  }

  @Test
  void refresh_withBlankToken_returnsBadRequest() throws Exception {
    RefreshTokenRequest request = new RefreshTokenRequest("");

    mockMvc
        .perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }
}
