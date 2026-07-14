package wallet;

import domain.outbox.AggregateType;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import domain.wallets.Wallet;
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
  private final OutboxEventRepository outboxEventRepository;

  @Override
  @Transactional
  public DepositResult execute(DepositInput input) {
    Wallet wallet = walletRepository.getByUserId(input.userId());

    WalletTransaction transaction = wallet.deposit(input.amount(), input.description());

    walletRepository.save(wallet);
    WalletTransaction saved = walletRepository.saveTransaction(transaction);

    outboxEventRepository.save(
        OutboxEvent.create(
            AggregateType.WALLET,
            wallet.getId(),
            EventType.WALLET_DEPOSITED,
            String.format(
                "{\"walletId\":\"%s\",\"userId\":\"%s\",\"amount\":\"%s\",\"balanceAfter\":\"%s\"}",
                wallet.getId(), input.userId(), saved.getAmount(), saved.getBalanceAfter())));

    return new DepositResult(
        saved.getId(),
        saved.getAmount(),
        saved.getBalanceAfter(),
        wallet.getCurrency(),
        saved.getType(),
        saved.getCreatedAt());
  }

  @Override
  public String errorMessage() {
    return "Error al realizar el depósito";
  }
}
