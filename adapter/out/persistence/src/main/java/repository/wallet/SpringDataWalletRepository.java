package repository.wallet;

import entity.wallet.WalletJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataWalletRepository extends JpaRepository<WalletJpaEntity, UUID> {
  Optional<WalletJpaEntity> findByUser_Id(UUID userId);
}
