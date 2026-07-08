package controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import auction.output.AuctionSummary;
import domain.auction.AuctionStatus;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import watchList.AddToWatchListUseCase;
import watchList.ListWatchListUseCase;
import watchList.RemoveFromWatchListUseCase;
import watchList.input.AddToWatchListInput;
import watchList.input.ListWatchListInput;
import watchList.input.RemoveFromWatchListInput;
import watchList.output.AddToWatchListResult;
import watchList.output.ListWatchListResult;
import watchList.output.ListWatchListResult.WatchListEntry;
import watchList.output.RemoveFromWatchListResult;

@ExtendWith(MockitoExtension.class)
class WatchListControllerTest {

  @Mock private AddToWatchListUseCase addToWatchListUseCase;
  @Mock private RemoveFromWatchListUseCase removeFromWatchListUseCase;
  @Mock private ListWatchListUseCase listWatchListUseCase;

  private MockMvc mockMvc;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    WatchListController controller =
        new WatchListController(
            addToWatchListUseCase, removeFromWatchListUseCase, listWatchListUseCase);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(userId.toString(), "password", "ROLE_BUYER"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void add_withAuthenticatedUser_returnsCreated() throws Exception {
    UUID auctionId = UUID.randomUUID();
    AddToWatchListResult result =
        new AddToWatchListResult(UUID.randomUUID(), auctionId, Instant.now());

    when(addToWatchListUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(post("/watchlist/{auctionId}", auctionId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(result.id().toString()))
        .andExpect(jsonPath("$.auctionId").value(auctionId.toString()));

    ArgumentCaptor<AddToWatchListInput> captor = ArgumentCaptor.forClass(AddToWatchListInput.class);
    verify(addToWatchListUseCase).run(captor.capture());
    assertThat(captor.getValue().userId()).isEqualTo(userId);
    assertThat(captor.getValue().auctionId()).isEqualTo(auctionId);
  }

  @Test
  void remove_withAuthenticatedUser_returnsOk() throws Exception {
    UUID auctionId = UUID.randomUUID();
    RemoveFromWatchListResult result = new RemoveFromWatchListResult(auctionId);

    when(removeFromWatchListUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(delete("/watchlist/{auctionId}", auctionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.auctionId").value(auctionId.toString()));

    ArgumentCaptor<RemoveFromWatchListInput> captor =
        ArgumentCaptor.forClass(RemoveFromWatchListInput.class);
    verify(removeFromWatchListUseCase).run(captor.capture());
    assertThat(captor.getValue().userId()).isEqualTo(userId);
    assertThat(captor.getValue().auctionId()).isEqualTo(auctionId);
  }

  @Test
  void list_withAuthenticatedUser_returnsOk() throws Exception {
    AuctionSummary auctionSummary =
        AuctionSummary.builder()
            .id(UUID.randomUUID())
            .title("Subasta test")
            .description("Descripcion")
            .categoryId(UUID.randomUUID())
            .sellerId(UUID.randomUUID())
            .currentPrice(BigDecimal.TEN)
            .status(AuctionStatus.ACTIVE)
            .startsAt(Instant.now())
            .endsAt(Instant.now().plusSeconds(3600))
            .build();
    WatchListEntry entry = new WatchListEntry(UUID.randomUUID(), Instant.now(), auctionSummary);

    when(listWatchListUseCase.run(any())).thenReturn(new ListWatchListResult(List.of(entry)));

    mockMvc
        .perform(get("/watchlist"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entries").isArray())
        .andExpect(jsonPath("$.entries", hasSize(1)))
        .andExpect(jsonPath("$.entries[0].watchListId").value(entry.watchListId().toString()));

    ArgumentCaptor<ListWatchListInput> captor = ArgumentCaptor.forClass(ListWatchListInput.class);
    verify(listWatchListUseCase).run(captor.capture());
    assertThat(captor.getValue().userId()).isEqualTo(userId);
  }
}
