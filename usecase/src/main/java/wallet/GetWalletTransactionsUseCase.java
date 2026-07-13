package wallet;

import domain.wallets.Wallet;
import domain.wallets.WalletRepository;
import domain.wallets.WalletTransaction;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;
import wallet.input.GetWalletTransactionsInput;
import wallet.output.GetWalletTransactionsResult;
import wallet.output.GetWalletTransactionsResult.WalletTransactionSummary;

@Service
@RequiredArgsConstructor
public class GetWalletTransactionsUseCase
    implements UseCase<GetWalletTransactionsInput, GetWalletTransactionsResult> {

  private final WalletRepository walletRepository;

  @Override
  public GetWalletTransactionsResult execute(GetWalletTransactionsInput input) {
    Wallet wallet = walletRepository.getByUserId(input.userId());

    List<WalletTransaction> transactions =
        walletRepository.findTransactionsByWalletId(wallet.getId());

    List<WalletTransactionSummary> summaries =
        transactions.stream()
            .map(
                tx ->
                    new WalletTransactionSummary(
                        tx.getId(),
                        tx.getReferenceId(),
                        tx.getType(),
                        tx.getAmount(),
                        tx.getBalanceAfter(),
                        tx.getDescription(),
                        tx.getCreatedAt()))
            .toList();

    return new GetWalletTransactionsResult(summaries);
  }

  @Override
  public String errorMessage() {
    return "Error al obtener el historial de transacciones";
  }
}
