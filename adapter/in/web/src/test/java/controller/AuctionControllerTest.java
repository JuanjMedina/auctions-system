package controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import auction.CancelAuctionUseCase;
import auction.CloseAuctionUseCase;
import auction.CreateAuctionUseCase;
import auction.GetAuctionUseCase;
import auction.ListAuctionsUseCase;
import auction.PublishAuctionUseCase;
import auction.input.CreateAuctionInput;
import auction.input.ListAuctionsInput;
import auction.output.AuctionSummary;
import auction.output.CreateAuctionResult;
import auction.output.ListAuctionsResult;
import controller.auction.AuctionController;
import controller.auction.dto.CreateAuctionRequest;
import domain.auction.AuctionStatus;
import domain.shared.PageResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

// Standalone: registra solo AuctionController con mocks Mockito, sin levantar
// ApplicationContext de Spring (no requiere @SpringBootConfiguration ni
// @WebMvcTest, y no arrastra otros controllers como BidController).
@ExtendWith(MockitoExtension.class)
class AuctionControllerTest {

  @Mock private CreateAuctionUseCase createAuctionUseCase;
  @Mock private GetAuctionUseCase getAuctionUseCase;
  @Mock private PublishAuctionUseCase publishAuctionUseCase;
  @Mock private CancelAuctionUseCase cancelAuctionUseCase;
  @Mock private CloseAuctionUseCase closeAuctionUseCase;
  @Mock private ListAuctionsUseCase listAuctionsUseCase;

  private MockMvc mockMvc;

  private ObjectMapper objectMapper = JsonMapper.builder().build();

  @BeforeEach
  void setUp() {
    AuctionController controller =
        new AuctionController(
            createAuctionUseCase,
            getAuctionUseCase,
            publishAuctionUseCase,
            cancelAuctionUseCase,
            closeAuctionUseCase,
            listAuctionsUseCase);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private AuctionSummary buildSummary(AuctionStatus status, UUID categoryId) {
    return AuctionSummary.builder()
        .id(UUID.randomUUID())
        .title("Subasta test")
        .description("Descripcion")
        .categoryId(categoryId)
        .sellerId(UUID.randomUUID())
        .currentPrice(BigDecimal.TEN)
        .status(status)
        .startsAt(Instant.now())
        .endsAt(Instant.now().plusSeconds(3600))
        .build();
  }

  @Test
  void listAuctions_noParams_returnsOkWithDefaultPage() throws Exception {
    PageResult<AuctionSummary> page =
        PageResult.<AuctionSummary>builder()
            .content(List.of(buildSummary(AuctionStatus.ACTIVE, UUID.randomUUID())))
            .totalElements(1L)
            .totalPages(1)
            .currentPage(0)
            .pageSize(10)
            .build();

    when(listAuctionsUseCase.run(any())).thenReturn(new ListAuctionsResult(page));

    mockMvc
        .perform(get("/auctions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.content").isArray())
        .andExpect(jsonPath("$.page.content", hasSize(1)))
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.page.currentPage").value(0))
        .andExpect(jsonPath("$.page.pageSize").value(10));

    ArgumentCaptor<ListAuctionsInput> captor = ArgumentCaptor.forClass(ListAuctionsInput.class);
    verify(listAuctionsUseCase).run(captor.capture());
    assertThat(captor.getValue().status()).isEmpty();
    assertThat(captor.getValue().categoryId()).isEmpty();
    assertThat(captor.getValue().page()).isNull();
    assertThat(captor.getValue().size()).isNull();
  }

  @Test
  void listAuctions_withFilters_forwardsThemToUseCase() throws Exception {
    UUID categoryId = UUID.randomUUID();
    PageResult<AuctionSummary> page =
        PageResult.<AuctionSummary>builder()
            .content(List.of())
            .totalElements(0L)
            .totalPages(0)
            .currentPage(1)
            .pageSize(5)
            .build();

    when(listAuctionsUseCase.run(any())).thenReturn(new ListAuctionsResult(page));

    mockMvc
        .perform(
            get("/auctions")
                .param("status", "ACTIVE")
                .param("categoryId", categoryId.toString())
                .param("page", "1")
                .param("size", "5"))
        .andExpect(status().isOk());

    ArgumentCaptor<ListAuctionsInput> captor = ArgumentCaptor.forClass(ListAuctionsInput.class);
    verify(listAuctionsUseCase).run(captor.capture());
    assertThat(captor.getValue().status()).contains(AuctionStatus.ACTIVE);
    assertThat(captor.getValue().categoryId()).contains(categoryId);
    assertThat(captor.getValue().page()).isEqualTo(1);
    assertThat(captor.getValue().size()).isEqualTo(5);
  }

  @Test
  void createAuction_withAuthentication_returnsCreated() throws Exception {
    // arrange
    UUID categoryId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    String sellerCredentials = "password";
    String sellerRole = "ROLE_SELLER";

    CreateAuctionRequest request =
        new CreateAuctionRequest(
            categoryId,
            "Subasta test",
            "Descripcion",
            BigDecimal.TEN,
            BigDecimal.valueOf(20),
            Instant.now().plusSeconds(60),
            Instant.now().plusSeconds(3600),
            true,
            10);

    CreateAuctionResult auctionResult =
        new CreateAuctionResult(UUID.randomUUID(), AuctionStatus.DRAFT, Instant.now());

    // SecurityFitlerChain mocks the authentication, so we need to set it manually for the test
    // works in single threaded test execution, but in parallel tests it may cause issues, so
    // consider using @DirtiesContext or other isolation strategies if needed.
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(sellerId.toString(), sellerCredentials, sellerRole));

    when(createAuctionUseCase.run(any())).thenReturn(auctionResult);

    // act
    mockMvc
        .perform(
            post("/auctions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(auctionResult.id().toString()))
        .andExpect(jsonPath("$.status").value(auctionResult.status().toString()))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());

    // assert
    ArgumentCaptor<CreateAuctionInput> captor = ArgumentCaptor.forClass(CreateAuctionInput.class);
    verify(createAuctionUseCase).run(captor.capture());
    assertThat(captor.getValue().categoryId()).isEqualTo(categoryId);
    assertThat(captor.getValue().sellerId()).isEqualTo(sellerId);
    assertThat(captor.getValue().autoExtend()).isTrue();
  }
}
