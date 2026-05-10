package repository.wallet;

import entity.wallet.WalletTransactionJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataWalletTransactionRepository
    extends JpaRepository<WalletTransactionJpaEntity, UUID> {
  List<WalletTransactionJpaEntity> findByWallet_IdOrderByCreatedAtDesc(UUID walletId);
}
