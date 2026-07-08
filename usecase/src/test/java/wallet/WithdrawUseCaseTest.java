package wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.wallets.TransactionType;
import domain.wallets.Wallet;
import domain.wallets.WalletExceptions;
import domain.wallets.WalletRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wallet.input.WithdrawInput;
import wallet.output.WithdrawResult;

@ExtendWith(MockitoExtension.class)
class WithdrawUseCaseTest {

  @Mock private WalletRepository walletRepository;

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
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
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
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
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
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WalletExceptions.WalletNotFoundException.class);
  }

  // --- fondos insuficientes ---

  @Test
  void execute_insufficientFunds_throwsInsufficientFundsException() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(10));
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WalletExceptions.InsufficientFundsException.class);
  }

  @Test
  void execute_insufficientFunds_neverSavesAnything() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(10));
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

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
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

    // act & assert
    assertThatThrownBy(() -> useCase.run(validInput()))
        .isInstanceOf(WalletExceptions.InsufficientFundsException.class);
  }
}
