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
import wallet.input.DepositInput;
import wallet.output.DepositResult;

@ExtendWith(MockitoExtension.class)
class DepositUseCaseTest {

  @Mock private WalletRepository walletRepository;
  @Mock private OutboxEventRepository outboxEventRepository;

  @InjectMocks private DepositUseCase useCase;

  // --- fixtures ---
  private static final UUID USER_ID = UUID.randomUUID();

  private Wallet buildWallet(BigDecimal balance) {
    return Wallet.reconstitute(
        UUID.randomUUID(), USER_ID, balance, BigDecimal.ZERO, "USD", 0L, Instant.now());
  }

  private DepositInput validInput() {
    return new DepositInput(USER_ID, BigDecimal.valueOf(100), "Top up");
  }

  // --- happy path ---

  @Test
  void execute_validDeposit_returnsDepositResult() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(50));
    when(walletRepository.getByUserId(USER_ID)).thenReturn(wallet);
    when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.saveTransaction(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    DepositResult result = useCase.run(validInput());

    // assert
    assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    assertThat(result.balanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(150));
    assertThat(result.currency()).isEqualTo("USD");
    assertThat(result.type()).isEqualTo(TransactionType.DEPOSIT);
    assertThat(result.transactionId()).isNotNull();
  }

  @Test
  void execute_validDeposit_persistsWalletAndTransaction() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(50));
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

  @Test
  void execute_walletNotFound_neverSavesAnything() {
    // arrange
    when(walletRepository.getByUserId(USER_ID))
        .thenThrow(new WalletExceptions.WalletNotFoundException(USER_ID));

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WalletExceptions.WalletNotFoundException.class);

    verify(walletRepository, never()).save(any());
    verify(walletRepository, never()).saveTransaction(any());
  }

  // --- monto inválido ---

  @Test
  void execute_negativeAmount_throwsIllegalArgumentException() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(50));
    when(walletRepository.getByUserId(USER_ID)).thenReturn(wallet);

    DepositInput input = new DepositInput(USER_ID, BigDecimal.valueOf(-10), "Invalid");

    // act & assert
    assertThatThrownBy(() -> useCase.run(input)).isInstanceOf(IllegalArgumentException.class);
  }

  // --- evento de outbox ---

  @Test
  void execute_validDeposit_emitsWalletDepositedEvent() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(50));
    when(walletRepository.getByUserId(USER_ID)).thenReturn(wallet);
    when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(walletRepository.saveTransaction(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    useCase.run(validInput());

    // assert
    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    OutboxEvent event = captor.getValue();
    assertThat(event.getEventType()).isEqualTo(EventType.WALLET_DEPOSITED);
    assertThat(event.getAggregateType()).isEqualTo(AggregateType.WALLET);
    assertThat(event.getAggregateId()).isEqualTo(wallet.getId());
  }
}
