package wallet;

import domain.wallets.Wallet;
import domain.wallets.WalletExceptions;
import domain.wallets.WalletRepository;
import domain.wallets.WalletTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.UseCase;
import wallet.input.DepositInput;
import wallet.output.DepositResult;

@Service
@RequiredArgsConstructor
public class DepositUseCase implements UseCase<DepositInput, DepositResult> {

  private final WalletRepository walletRepository;

  @Override
  @Transactional
  public DepositResult execute(DepositInput input) {
    Wallet wallet =
        walletRepository
            .findByUserId(input.userId())
            .orElseThrow(() -> new WalletExceptions.WalletNotFoundException(input.userId()));

    WalletTransaction transaction = wallet.deposit(input.amount(), input.description());

    walletRepository.save(wallet);
    WalletTransaction saved = walletRepository.saveTransaction(transaction);

    return new DepositResult(
        saved.getId(),
        saved.getAmount(),
        saved.getBalanceAfter(),
        wallet.getCurrency(),
        saved.getType(),
        saved.getCreatedAt());
  }

  @Override
  public DepositResult failed(Exception exception) {
    if (exception instanceof WalletExceptions.WalletNotFoundException e) throw e;
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al realizar el depósito", exception);
  }
}
