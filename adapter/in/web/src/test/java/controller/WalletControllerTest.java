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

import controller.wallet.WalletController;
import controller.wallet.dto.DepositRequest;
import controller.wallet.dto.WithdrawRequest;
import domain.wallets.TransactionType;
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
import wallet.DepositUseCase;
import wallet.GetWalletTransactionsUseCase;
import wallet.GetWalletUseCase;
import wallet.WithdrawUseCase;
import wallet.input.DepositInput;
import wallet.input.GetWalletInput;
import wallet.input.GetWalletTransactionsInput;
import wallet.input.WithdrawInput;
import wallet.output.DepositResult;
import wallet.output.GetWalletResult;
import wallet.output.GetWalletTransactionsResult;
import wallet.output.GetWalletTransactionsResult.WalletTransactionSummary;
import wallet.output.WithdrawResult;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

  @Mock private GetWalletUseCase getWalletUseCase;
  @Mock private DepositUseCase depositUseCase;
  @Mock private WithdrawUseCase withdrawUseCase;
  @Mock private GetWalletTransactionsUseCase getWalletTransactionsUseCase;

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    WalletController controller =
        new WalletController(
            getWalletUseCase, depositUseCase, withdrawUseCase, getWalletTransactionsUseCase);
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
  void getWallet_withAuthenticatedUser_returnsOk() throws Exception {
    GetWalletResult result =
        new GetWalletResult(
            UUID.randomUUID(),
            userId,
            BigDecimal.valueOf(100),
            BigDecimal.ZERO,
            BigDecimal.valueOf(100),
            "USD",
            Instant.now());

    when(getWalletUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(get("/wallets/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.balance").value(100));

    ArgumentCaptor<GetWalletInput> captor = ArgumentCaptor.forClass(GetWalletInput.class);
    verify(getWalletUseCase).run(captor.capture());
    assertThat(captor.getValue().userId()).isEqualTo(userId);
  }

  @Test
  void deposit_withValidRequest_returnsOk() throws Exception {
    DepositRequest request = new DepositRequest(BigDecimal.valueOf(50), "Top up");

    DepositResult result =
        new DepositResult(
            UUID.randomUUID(),
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(150),
            "USD",
            TransactionType.DEPOSIT,
            Instant.now());

    when(depositUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(
            post("/wallets/me/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(50))
        .andExpect(jsonPath("$.balanceAfter").value(150))
        .andExpect(jsonPath("$.type").value("DEPOSIT"));

    ArgumentCaptor<DepositInput> captor = ArgumentCaptor.forClass(DepositInput.class);
    verify(depositUseCase).run(captor.capture());
    assertThat(captor.getValue().userId()).isEqualTo(userId);
    assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
    assertThat(captor.getValue().description()).isEqualTo("Top up");
  }

  @Test
  void deposit_withNonPositiveAmount_returnsBadRequest() throws Exception {
    DepositRequest request = new DepositRequest(BigDecimal.ZERO, "Top up");

    mockMvc
        .perform(
            post("/wallets/me/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void withdraw_withValidRequest_returnsOk() throws Exception {
    WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(30), "Cash out");

    WithdrawResult result =
        new WithdrawResult(
            UUID.randomUUID(),
            BigDecimal.valueOf(30),
            BigDecimal.valueOf(70),
            "USD",
            TransactionType.WITHDRAWAL,
            Instant.now());

    when(withdrawUseCase.run(any())).thenReturn(result);

    mockMvc
        .perform(
            post("/wallets/me/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(30))
        .andExpect(jsonPath("$.balanceAfter").value(70))
        .andExpect(jsonPath("$.type").value("WITHDRAWAL"));

    ArgumentCaptor<WithdrawInput> captor = ArgumentCaptor.forClass(WithdrawInput.class);
    verify(withdrawUseCase).run(captor.capture());
    assertThat(captor.getValue().userId()).isEqualTo(userId);
    assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(30));
    assertThat(captor.getValue().description()).isEqualTo("Cash out");
  }

  @Test
  void withdraw_withNonPositiveAmount_returnsBadRequest() throws Exception {
    WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(-5), "Cash out");

    mockMvc
        .perform(
            post("/wallets/me/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void transactions_withAuthenticatedUser_returnsOk() throws Exception {
    WalletTransactionSummary summary =
        new WalletTransactionSummary(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TransactionType.DEPOSIT,
            BigDecimal.TEN,
            BigDecimal.valueOf(110),
            "desc",
            Instant.now());

    when(getWalletTransactionsUseCase.run(any()))
        .thenReturn(new GetWalletTransactionsResult(List.of(summary)));

    mockMvc
        .perform(get("/wallets/me/transactions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactions").isArray())
        .andExpect(jsonPath("$.transactions", hasSize(1)))
        .andExpect(jsonPath("$.transactions[0].id").value(summary.id().toString()));

    ArgumentCaptor<GetWalletTransactionsInput> captor =
        ArgumentCaptor.forClass(GetWalletTransactionsInput.class);
    verify(getWalletTransactionsUseCase).run(captor.capture());
    assertThat(captor.getValue().userId()).isEqualTo(userId);
  }
}
