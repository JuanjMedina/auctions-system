package wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.wallets.TransactionType;
import domain.wallets.Wallet;
import domain.wallets.WalletExceptions;
import domain.wallets.WalletRepository;
import domain.wallets.WalletTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wallet.input.GetWalletTransactionsInput;
import wallet.output.GetWalletTransactionsResult;

@ExtendWith(MockitoExtension.class)
class GetWalletTransactionsUseCaseTest {

  @Mock private WalletRepository walletRepository;

  @InjectMocks private GetWalletTransactionsUseCase useCase;

  // --- fixtures ---
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID WALLET_ID = UUID.randomUUID();

  private Wallet buildWallet() {
    return Wallet.reconstitute(
        WALLET_ID, USER_ID, BigDecimal.valueOf(100), BigDecimal.ZERO, "USD", 0L, Instant.now());
  }

  private WalletTransaction buildTransaction(TransactionType type, BigDecimal amount) {
    return WalletTransaction.reconstitute(
        UUID.randomUUID(),
        WALLET_ID,
        null,
        type,
        amount,
        BigDecimal.valueOf(100),
        "desc",
        Instant.now());
  }

  // --- happy path ---

  @Test
  void execute_walletWithTransactions_returnsAllTransactions() {
    // arrange
    Wallet wallet = buildWallet();
    List<WalletTransaction> transactions =
        List.of(
            buildTransaction(TransactionType.DEPOSIT, BigDecimal.valueOf(50)),
            buildTransaction(TransactionType.WITHDRAWAL, BigDecimal.valueOf(20)));

    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
    when(walletRepository.findTransactionsByWalletId(WALLET_ID)).thenReturn(transactions);

    // act
    GetWalletTransactionsResult result = useCase.run(new GetWalletTransactionsInput(USER_ID));

    // assert
    assertThat(result.transactions()).hasSize(2);
  }

  @Test
  void execute_walletWithTransactions_mapsFieldsCorrectly() {
    // arrange
    Wallet wallet = buildWallet();
    WalletTransaction transaction =
        buildTransaction(TransactionType.DEPOSIT, BigDecimal.valueOf(50));

    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
    when(walletRepository.findTransactionsByWalletId(WALLET_ID)).thenReturn(List.of(transaction));

    // act
    GetWalletTransactionsResult result = useCase.run(new GetWalletTransactionsInput(USER_ID));

    // assert
    var summary = result.transactions().get(0);
    assertThat(summary.id()).isEqualTo(transaction.getId());
    assertThat(summary.type()).isEqualTo(TransactionType.DEPOSIT);
    assertThat(summary.amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
    assertThat(summary.balanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(100));
    assertThat(summary.description()).isEqualTo("desc");
  }

  // --- lista vacía ---

  @Test
  void execute_walletWithNoTransactions_returnsEmptyList() {
    // arrange
    Wallet wallet = buildWallet();
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
    when(walletRepository.findTransactionsByWalletId(WALLET_ID)).thenReturn(List.of());

    // act
    GetWalletTransactionsResult result = useCase.run(new GetWalletTransactionsInput(USER_ID));

    // assert
    assertThat(result.transactions()).isEmpty();
  }

  // --- billetera no encontrada ---

  @Test
  void execute_walletNotFound_throwsWalletNotFoundException() {
    // arrange
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(new GetWalletTransactionsInput(USER_ID)))
        .isInstanceOf(WalletExceptions.WalletNotFoundException.class);
  }

  @Test
  void execute_walletNotFound_neverQueriesTransactions() {
    // arrange
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(new GetWalletTransactionsInput(USER_ID)))
        .isInstanceOf(WalletExceptions.WalletNotFoundException.class);

    verify(walletRepository, never())
        .findTransactionsByWalletId(org.mockito.ArgumentMatchers.any());
  }
}
