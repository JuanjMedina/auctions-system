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

import bid.ListBidsUseCase;
import bid.PlaceBidUseCase;
import bid.input.ListBidsInput;
import bid.input.PlaceBidInput;
import bid.output.ListBidsResult;
import bid.output.ListBidsResult.BidSummary;
import bid.output.PlaceBidOutput;
import controller.bid.BidController;
import controller.bid.dto.PlaceBidRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class BidControllerTest {

  @Mock private PlaceBidUseCase placeBidUseCase;
  @Mock private ListBidsUseCase listBidsUseCase;

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  private final UUID bidderId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    BidController controller = new BidController(placeBidUseCase, listBidsUseCase);
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
  void placeBid_withAuthenticatedBuyer_returnsCreated() throws Exception {
    UUID auctionId = UUID.randomUUID();
    PlaceBidRequest request = new PlaceBidRequest(BigDecimal.valueOf(50), false, null);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(bidderId.toString(), "password", "ROLE_BUYER"));

    PlaceBidOutput output =
        new PlaceBidOutput(
            UUID.randomUUID(), auctionId, BigDecimal.valueOf(50), BidStatus.WINNING, Instant.now());

    when(placeBidUseCase.run(any())).thenReturn(output);

    mockMvc
        .perform(
            post("/auctions/{auctionId}/bids", auctionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.bidId").value(output.bidId().toString()))
        .andExpect(jsonPath("$.auctionId").value(auctionId.toString()))
        .andExpect(jsonPath("$.status").value("WINNING"));

    ArgumentCaptor<PlaceBidInput> captor = ArgumentCaptor.forClass(PlaceBidInput.class);
    verify(placeBidUseCase).run(captor.capture());
    assertThat(captor.getValue().auctionId()).isEqualTo(auctionId);
    assertThat(captor.getValue().bidderId()).isEqualTo(bidderId);
    assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
    assertThat(captor.getValue().autoBid()).isFalse();
  }

  @Test
  void placeBid_withNonPositiveAmount_returnsBadRequest() throws Exception {
    UUID auctionId = UUID.randomUUID();
    PlaceBidRequest request = new PlaceBidRequest(BigDecimal.ZERO, false, null);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(bidderId.toString(), "password", "ROLE_BUYER"));

    mockMvc
        .perform(
            post("/auctions/{auctionId}/bids", auctionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listBids_returnsOkWithBids() throws Exception {
    UUID auctionId = UUID.randomUUID();
    BidSummary summary =
        new BidSummary(
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.TEN,
            false,
            BidStatus.WINNING,
            Instant.now());

    when(listBidsUseCase.run(any())).thenReturn(new ListBidsResult(List.of(summary)));

    mockMvc
        .perform(get("/auctions/{auctionId}/bids", auctionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bids").isArray())
        .andExpect(jsonPath("$.bids", hasSize(1)))
        .andExpect(jsonPath("$.bids[0].bidId").value(summary.bidId().toString()));

    ArgumentCaptor<ListBidsInput> captor = ArgumentCaptor.forClass(ListBidsInput.class);
    verify(listBidsUseCase).run(captor.capture());
    assertThat(captor.getValue().auctionId()).isEqualTo(auctionId);
  }
}
