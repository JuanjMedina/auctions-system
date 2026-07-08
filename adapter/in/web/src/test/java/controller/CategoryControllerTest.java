package controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import category.CreateCategoryUseCase;
import category.GetCategoryUseCase;
import category.input.CreateCategoryInput;
import category.input.GetCategoryInput;
import category.output.CreateCategoryResult;
import category.output.GetCategoryResult;
import controller.dto.request.CreateCategoryRequest;
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

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

  @Mock private CreateCategoryUseCase createCategoryUseCase;
  @Mock private GetCategoryUseCase getCategoryUseCase;

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @BeforeEach
  void setUp() {
    CategoryController controller =
        new CategoryController(createCategoryUseCase, getCategoryUseCase);
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
  void create_withAdminAuthentication_returnsCreated() throws Exception {
    UUID parentId = UUID.randomUUID();
    CreateCategoryRequest request =
        new CreateCategoryRequest("Electronics", "electronics", parentId);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(UUID.randomUUID().toString(), "password", "ROLE_ADMIN"));

    CreateCategoryResult result =
        new CreateCategoryResult(UUID.randomUUID(), "Electronics", "electronics", parentId, true);

    when(createCategoryUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(
            post("/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(result.id().toString()))
        .andExpect(jsonPath("$.name").value("Electronics"))
        .andExpect(jsonPath("$.slug").value("electronics"))
        .andExpect(jsonPath("$.isActive").value(true));

    ArgumentCaptor<CreateCategoryInput> captor = ArgumentCaptor.forClass(CreateCategoryInput.class);
    verify(createCategoryUseCase).run(captor.capture());
    assertThat(captor.getValue().name()).isEqualTo("Electronics");
    assertThat(captor.getValue().slug()).isEqualTo("electronics");
    assertThat(captor.getValue().parentId()).isEqualTo(parentId);
  }

  @Test
  void create_withBlankName_returnsBadRequest() throws Exception {
    CreateCategoryRequest request = new CreateCategoryRequest("", "electronics", null);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(UUID.randomUUID().toString(), "password", "ROLE_ADMIN"));

    mockMvc
        .perform(
            post("/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void get_withExistingId_returnsOk() throws Exception {
    UUID categoryId = UUID.randomUUID();
    GetCategoryResult result =
        new GetCategoryResult(categoryId, "Electronics", "electronics", null, true);

    when(getCategoryUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(get("/categories/{id}", categoryId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(categoryId.toString()))
        .andExpect(jsonPath("$.name").value("Electronics"));

    ArgumentCaptor<GetCategoryInput> captor = ArgumentCaptor.forClass(GetCategoryInput.class);
    verify(getCategoryUseCase).run(captor.capture());
    assertThat(captor.getValue().categoryId()).isEqualTo(categoryId);
  }
}
