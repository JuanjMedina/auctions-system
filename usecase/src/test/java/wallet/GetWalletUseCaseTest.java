package wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
import wallet.input.GetWalletInput;
import wallet.output.GetWalletResult;

@ExtendWith(MockitoExtension.class)
class GetWalletUseCaseTest {

  @Mock private WalletRepository walletRepository;

  @InjectMocks private GetWalletUseCase useCase;

  // --- fixtures ---
  private static final UUID USER_ID = UUID.randomUUID();

  private Wallet buildWallet(BigDecimal balance, BigDecimal reserved) {
    return Wallet.reconstitute(
        UUID.randomUUID(), USER_ID, balance, reserved, "USD", 0L, Instant.now());
  }

  // --- happy path ---

  @Test
  void execute_existingWallet_returnsWalletData() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(100), BigDecimal.valueOf(20));
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

    // act
    GetWalletResult result = useCase.run(new GetWalletInput(USER_ID));

    // assert
    assertThat(result.userId()).isEqualTo(USER_ID);
    assertThat(result.balance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    assertThat(result.reservedBalance()).isEqualByComparingTo(BigDecimal.valueOf(20));
    assertThat(result.currency()).isEqualTo("USD");
  }

  @Test
  void execute_walletWithReservedFunds_computesAvailableBalanceCorrectly() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(100), BigDecimal.valueOf(20));
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

    // act
    GetWalletResult result = useCase.run(new GetWalletInput(USER_ID));

    // assert
    assertThat(result.availableBalance()).isEqualByComparingTo(BigDecimal.valueOf(80));
  }

  @Test
  void execute_walletWithNoReservedFunds_availableBalanceEqualsBalance() {
    // arrange
    Wallet wallet = buildWallet(BigDecimal.valueOf(100), BigDecimal.ZERO);
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

    // act
    GetWalletResult result = useCase.run(new GetWalletInput(USER_ID));

    // assert
    assertThat(result.availableBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
  }

  // --- billetera no encontrada ---

  @Test
  void execute_walletNotFound_throwsWalletNotFoundException() {
    // arrange
    when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> useCase.run(new GetWalletInput(USER_ID)))
        .isInstanceOf(WalletExceptions.WalletNotFoundException.class);
  }
}
