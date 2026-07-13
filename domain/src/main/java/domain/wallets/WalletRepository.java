package domain.wallets;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
  Wallet save(Wallet wallet);

  Optional<Wallet> findById(UUID id);

  Optional<Wallet> findByUserId(UUID userId);

  default Wallet getByUserId(UUID userId) {
    return findByUserId(userId)
        .orElseThrow(() -> new WalletExceptions.WalletNotFoundException(userId));
  }

  WalletTransaction saveTransaction(WalletTransaction transaction);

  List<WalletTransaction> findTransactionsByWalletId(UUID walletId);
}
