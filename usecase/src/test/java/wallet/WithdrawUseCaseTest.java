package wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.outbox.AggregateType;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import domain.wallets.TransactionType;
import domain.wallets.Wallet;
import domain.wallets.WalletExceptions;
import domain.wallets.WalletRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wallet.input.WithdrawInput;
import wallet.output.WithdrawResult;

@ExtendWith(MockitoExtension.class)
class WithdrawUseCaseTest {

  @Mock private WalletRepository walletRepository;
  @Mock private OutboxEventRepository outboxEventRepository;

  @InjectMocks private WithdrawUseCase useCase;

  // --- fixtures ---
  private static final UUID USER_ID = UUID.randomUUID();

  private Wallet buildWallet(BigDecimal balance) {
    return Wallet.reconstitute(
        UUID.randomUUID(), USER_ID, balance, BigDecimal.ZERO, "USD", 0L, Instant.now());
  }

  private WithdrawInput validInput() {
    return new WithdrawInput(USER_ID, BigDecimal.valueOf(30), "Cash out");
  }

  // --- happy path ---

  @Test
  void execute_validWithdraw_returnsWithdrawResult() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(100));
    when(walletRepository.getByUserId(USER_ID)).thenReturn(wallet);
    when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.saveTransaction(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    WithdrawResult result = useCase.run(validInput());

    // assert
    assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(30));
    assertThat(result.balanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(70));
    assertThat(result.currency()).isEqualTo("USD");
    assertThat(result.type()).isEqualTo(TransactionType.WITHDRAWAL);
  }

  @Test
  void execute_validWithdraw_persistsWalletAndTransaction() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(100));
    when(walletRepository.getByUserId(USER_ID)).thenReturn(wallet);
    when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.saveTransaction(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    useCase.run(validInput());

    // assert
    verify(walletRepository).save(wallet);
    verify(walletRepository).saveTransaction(any());
  }

  // --- billetera no encontrada ---

  @Test
  void execute_walletNotFound_throwsWalletNotFoundException() {
    // arrange
    when(walletRepository.getByUserId(USER_ID))
        .thenThrow(new WalletExceptions.WalletNotFoundException(USER_ID));

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WalletExceptions.WalletNotFoundException.class);
  }

  // --- fondos insuficientes ---

  @Test
  void execute_insufficientFunds_throwsInsufficientFundsException() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(10));
    when(walletRepository.getByUserId(USER_ID)).thenReturn(wallet);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WalletExceptions.InsufficientFundsException.class);
  }

  @Test
  void execute_insufficientFunds_neverSavesAnything() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(10));
    when(walletRepository.getByUserId(USER_ID)).thenReturn(wallet);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WalletExceptions.InsufficientFundsException.class);

    verify(walletRepository, never()).save(any());
    verify(walletRepository, never()).saveTransaction(any());
  }

  @Test
  void execute_withdrawWithReservedFunds_considersOnlyAvailableBalance() {
    // arrange: balance 100 but 90 reserved -> available balance is only 10
    Wallet wallet =
        Wallet.reconstitute(
            UUID.randomUUID(),
            USER_ID,
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(90),
            "USD",
            0L,
            Instant.now());
    when(walletRepository.getByUserId(USER_ID)).thenReturn(wallet);

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WalletExceptions.InsufficientFundsException.class);
  }

  // --- evento de outbox ---

  @Test
  void execute_validWithdraw_emitsWalletWithdrawnEvent() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(100));
    when(walletRepository.getByUserId(USER_ID)).thenReturn(wallet);
    when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.saveTransaction(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    useCase.run(validInput());

    // assert
    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    OutboxEvent event = captor.getValue();
    assertThat(event.getEventType()).isEqualTo(EventType.WALLET_WITHDRAWN);
    assertThat(event.getAggregateType()).isEqualTo(AggregateType.WALLET);
    assertThat(event.getAggregateId()).isEqualTo(wallet.getId());
  }
}
