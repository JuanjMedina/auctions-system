package wallet;

import domain.wallets.Wallet;
import domain.wallets.WalletExceptions;
import domain.wallets.WalletRepository;
import domain.wallets.WalletTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;
import wallet.input.WithdrawInput;
import wallet.output.WithdrawResult;

@Service
@RequiredArgsConstructor
public class WithdrawUseCase implements UseCase<WithdrawInput, WithdrawResult> {

  private final WalletRepository walletRepository;

  @Override
  @Transactional
  public WithdrawResult execute(WithdrawInput input) {
    Wallet wallet =
        walletRepository
            .findByUserId(input.userId())
            .orElseThrow(() -> new WalletExceptions.WalletNotFoundException(input.userId()));

    WalletTransaction transaction = wallet.withdraw(input.amount(), input.description());

    walletRepository.save(wallet);
    WalletTransaction saved = walletRepository.saveTransaction(transaction);

    return new WithdrawResult(
        saved.getId(),
        saved.getAmount(),
        saved.getBalanceAfter(),
        wallet.getCurrency(),
        saved.getType(),
        saved.getCreatedAt());
  }

  @Override
  public WithdrawResult failed(Exception exception) {
    if (exception instanceof WalletExceptions.WalletNotFoundException e) throw e;
    if (exception instanceof WalletExceptions.InsufficientFundsException e) throw e;
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al realizar el retiro", exception);
  }
}
