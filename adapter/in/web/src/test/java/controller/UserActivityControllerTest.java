package controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import auction.ListMyAuctionsUseCase;
import auction.input.ListMyAuctionsInput;
import auction.output.AuctionSummary;
import auction.output.ListMyAuctionsResult;
import bid.ListMyBidsUseCase;
import bid.input.ListMyBidsInput;
import bid.output.ListMyBidsResult;
import bid.output.ListMyBidsResult.MyBidSummary;
import controller.user.UserActivityController;
import domain.auction.AuctionStatus;
import domain.bid.BidStatus;
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

@ExtendWith(MockitoExtension.class)
class UserActivityControllerTest {

  @Mock private ListMyBidsUseCase listMyBidsUseCase;
  @Mock private ListMyAuctionsUseCase listMyAuctionsUseCase;

  private MockMvc mockMvc;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    UserActivityController controller =
        new UserActivityController(listMyBidsUseCase, listMyAuctionsUseCase);
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
  void myBids_withAuthenticatedUser_returnsOk() throws Exception {
    MyBidSummary summary =
        new MyBidSummary(
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.TEN,
            false,
            BidStatus.WINNING,
            Instant.now());

    when(listMyBidsUseCase.run(any())).thenReturn(new ListMyBidsResult(List.of(summary)));

    mockMvc
        .perform(get("/users/me/bids"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bids").isArray())
        .andExpect(jsonPath("$.bids", hasSize(1)))
        .andExpect(jsonPath("$.bids[0].bidId").value(summary.bidId().toString()));

    ArgumentCaptor<ListMyBidsInput> captor = ArgumentCaptor.forClass(ListMyBidsInput.class);
    verify(listMyBidsUseCase).run(captor.capture());
    assertThat(captor.getValue().bidderId()).isEqualTo(userId);
  }

  @Test
  void myAuctions_withAuthenticatedUser_returnsOk() throws Exception {
    AuctionSummary summary =
        AuctionSummary.builder()
            .id(UUID.randomUUID())
            .title("Subasta test")
            .description("Descripcion")
            .categoryId(UUID.randomUUID())
            .sellerId(userId)
            .currentPrice(BigDecimal.TEN)
            .status(AuctionStatus.ACTIVE)
            .startsAt(Instant.now())
            .endsAt(Instant.now().plusSeconds(3600))
            .build();

    when(listMyAuctionsUseCase.run(any())).thenReturn(new ListMyAuctionsResult(List.of(summary)));

    mockMvc
        .perform(get("/users/me/auctions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.auctions").isArray())
        .andExpect(jsonPath("$.auctions", hasSize(1)))
        .andExpect(jsonPath("$.auctions[0].id").value(summary.id().toString()));

    ArgumentCaptor<ListMyAuctionsInput> captor = ArgumentCaptor.forClass(ListMyAuctionsInput.class);
    verify(listMyAuctionsUseCase).run(captor.capture());
    assertThat(captor.getValue().sellerId()).isEqualTo(userId);
  }
}
