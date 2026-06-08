package adapter.wallet;

import domain.wallets.Wallet;
import domain.wallets.WalletRepository;
import domain.wallets.WalletTransaction;
import entity.user.UserEntity;
import entity.wallet.WalletJpaEntity;
import entity.wallet.WalletTransactionJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import repository.wallet.SpringDataWalletRepository;
import repository.wallet.SpringDataWalletTransactionRepository;

@Component
@RequiredArgsConstructor
public class WalletJpaAdapter implements WalletRepository {

  private final SpringDataWalletRepository walletRepo;
  private final SpringDataWalletTransactionRepository transactionRepo;

  @Override
  public Wallet save(Wallet wallet) {
    return toDomain(walletRepo.save(toJpaEntity(wallet)));
  }

  @Override
  public Optional<Wallet> findById(UUID id) {
    return walletRepo.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<Wallet> findByUserId(UUID userId) {
    return walletRepo.findByUser_Id(userId).map(this::toDomain);
  }

  @Override
  public WalletTransaction saveTransaction(WalletTransaction transaction) {
    WalletJpaEntity walletEntity =
        walletRepo
            .findById(transaction.getWalletId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException("Wallet not found: " + transaction.getWalletId()));
    return toDomain(transactionRepo.save(toJpaEntity(transaction, walletEntity)));
  }

  @Override
  public List<WalletTransaction> findTransactionsByWalletId(UUID walletId) {
    return transactionRepo.findByWallet_IdOrderByCreatedAtDesc(walletId).stream()
        .map(this::toDomain)
        .toList();
  }

  private WalletJpaEntity toJpaEntity(Wallet wallet) {
    return WalletJpaEntity.builder()
        .id(wallet.getId())
        .user(UserEntity.builder().id(wallet.getUserId()).build())
        .balance(wallet.getBalance())
        .reservedBalance(wallet.getReservedBalance())
        .currency(wallet.getCurrency())
        .version(wallet.getVersion())
        .build();
  }

  private Wallet toDomain(WalletJpaEntity entity) {
    return Wallet.reconstitute(
        entity.getId(),
        entity.getUserId(),
        entity.getBalance(),
        entity.getReservedBalance(),
        entity.getCurrency(),
        entity.getVersion(),
        entity.getUpdatedAt());
  }

  private WalletTransactionJpaEntity toJpaEntity(WalletTransaction tx, WalletJpaEntity wallet) {
    return WalletTransactionJpaEntity.builder()
        .id(tx.getId())
        .wallet(wallet)
        .referenceId(tx.getReferenceId())
        .type(tx.getType())
        .amount(tx.getAmount())
        .balanceAfter(tx.getBalanceAfter())
        .description(tx.getDescription())
        .createdAt(tx.getCreatedAt())
        .build();
  }

  private WalletTransaction toDomain(WalletTransactionJpaEntity entity) {
    return WalletTransaction.reconstitute(
        entity.getId(),
        entity.getWallet().getId(),
        entity.getReferenceId(),
        entity.getType(),
        entity.getAmount(),
        entity.getBalanceAfter(),
        entity.getDescription(),
        entity.getCreatedAt());
  }
}
