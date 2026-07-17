package adapter.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.shared.ConcurrencyException;
import domain.wallets.TransactionType;
import domain.wallets.Wallet;
import domain.wallets.WalletTransaction;
import entity.user.UserEntity;
import entity.wallet.WalletJpaEntity;
import entity.wallet.WalletTransactionJpaEntity;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import repository.wallet.SpringDataWalletRepository;
import repository.wallet.SpringDataWalletTransactionRepository;

@ExtendWith(MockitoExtension.class)
class WalletJpaAdapterTest {

  @Mock private SpringDataWalletRepository walletRepo;

  @Mock private SpringDataWalletTransactionRepository transactionRepo;

  @Mock private EntityManager entityManager;

  private WalletJpaAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new WalletJpaAdapter(walletRepo, transactionRepo, entityManager);
  }

  private Wallet buildWallet(UUID id, UUID userId) {
    return Wallet.reconstitute(
        id, userId, BigDecimal.valueOf(500), BigDecimal.valueOf(50), "USD", 2L, Instant.now());
  }

  private WalletJpaEntity buildEntity(UUID id, UUID userId) {
    return WalletJpaEntity.builder()
        .id(id)
        .user(UserEntity.builder().id(userId).build())
        .userId(userId)
        .balance(BigDecimal.valueOf(500))
        .reservedBalance(BigDecimal.valueOf(50))
        .currency("USD")
        .version(2L)
        .updatedAt(Instant.now())
        .build();
  }

  private WalletTransaction buildTransaction(UUID id, UUID walletId, UUID referenceId) {
    return WalletTransaction.reconstitute(
        id,
        walletId,
        referenceId,
        TransactionType.DEPOSIT,
        BigDecimal.valueOf(100),
        BigDecimal.valueOf(600),
        "Deposit test",
        Instant.now());
  }

  private WalletTransactionJpaEntity buildTransactionEntity(
      UUID id, WalletJpaEntity wallet, UUID referenceId) {
    return WalletTransactionJpaEntity.builder()
        .id(id)
        .wallet(wallet)
        .referenceId(referenceId)
        .type(TransactionType.DEPOSIT)
        .amount(BigDecimal.valueOf(100))
        .balanceAfter(BigDecimal.valueOf(600))
        .description("Deposit test")
        .createdAt(Instant.now())
        .build();
  }

  // --- save ---

  @Test
  void save_validWallet_delegatesToWalletRepoAndMapsResult() {
    // arrange
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Wallet wallet = buildWallet(id, userId);
    WalletJpaEntity savedEntity = buildEntity(id, userId);

    when(entityManager.getReference(UserEntity.class, userId))
        .thenReturn(UserEntity.builder().id(userId).build());
    when(walletRepo.save(any(WalletJpaEntity.class))).thenReturn(savedEntity);

    // act
    Wallet result = adapter.save(wallet);

    // assert
    ArgumentCaptor<WalletJpaEntity> captor = ArgumentCaptor.forClass(WalletJpaEntity.class);
    verify(walletRepo).save(captor.capture());
    WalletJpaEntity captured = captor.getValue();

    assertThat(captured.getId()).isEqualTo(id);
    assertThat(captured.getUser().getId()).isEqualTo(userId);
    assertThat(captured.getBalance()).isEqualByComparingTo(wallet.getBalance());
    assertThat(captured.getReservedBalance()).isEqualByComparingTo(wallet.getReservedBalance());
    assertThat(captured.getCurrency()).isEqualTo("USD");
    assertThat(captured.getVersion()).isEqualTo(2L);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(500));
  }

  @Test
  void save_optimisticLockingFailure_throwsConcurrencyException() {
    // arrange
    Wallet wallet = buildWallet(UUID.randomUUID(), UUID.randomUUID());
    when(walletRepo.save(any(WalletJpaEntity.class)))
        .thenThrow(new OptimisticLockingFailureException("conflict"));

    // act & assert
    assertThatThrownBy(() -> adapter.save(wallet)).isInstanceOf(ConcurrencyException.class);
  }

  // --- findById ---

  @Test
  void findById_existingWallet_returnsMappedDomain() {
    // arrange
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    WalletJpaEntity entity = buildEntity(id, userId);
    when(walletRepo.findById(id)).thenReturn(Optional.of(entity));

    // act
    Optional<Wallet> result = adapter.findById(id);

    // assert
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(id);
    assertThat(result.get().getUserId()).isEqualTo(userId);
  }

  @Test
  void findById_missingWallet_returnsEmptyOptional() {
    // arrange
    UUID id = UUID.randomUUID();
    when(walletRepo.findById(id)).thenReturn(Optional.empty());

    // act
    Optional<Wallet> result = adapter.findById(id);

    // assert
    assertThat(result).isEmpty();
  }

  // --- findByUserId ---

  @Test
  void findByUserId_existingWallet_returnsMappedDomain() {
    // arrange
    UUID userId = UUID.randomUUID();
    WalletJpaEntity entity = buildEntity(UUID.randomUUID(), userId);
    when(walletRepo.findByUser_Id(userId)).thenReturn(Optional.of(entity));

    // act
    Optional<Wallet> result = adapter.findByUserId(userId);

    // assert
    verify(walletRepo).findByUser_Id(eq(userId));
    assertThat(result).isPresent();
    assertThat(result.get().getUserId()).isEqualTo(userId);
  }

  @Test
  void findByUserId_missingWallet_returnsEmptyOptional() {
    // arrange
    UUID userId = UUID.randomUUID();
    when(walletRepo.findByUser_Id(userId)).thenReturn(Optional.empty());

    // act
    Optional<Wallet> result = adapter.findByUserId(userId);

    // assert
    assertThat(result).isEmpty();
  }

  // --- saveTransaction ---

  @Test
  void saveTransaction_existingWallet_delegatesAndMapsResult() {
    // arrange
    UUID walletId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    UUID referenceId = UUID.randomUUID();
    WalletJpaEntity walletEntity = buildEntity(walletId, UUID.randomUUID());
    WalletTransaction transaction = buildTransaction(transactionId, walletId, referenceId);
    WalletTransactionJpaEntity savedTxEntity =
        buildTransactionEntity(transactionId, walletEntity, referenceId);

    when(walletRepo.findById(walletId)).thenReturn(Optional.of(walletEntity));
    when(transactionRepo.save(any(WalletTransactionJpaEntity.class))).thenReturn(savedTxEntity);

    // act
    WalletTransaction result = adapter.saveTransaction(transaction);

    // assert
    ArgumentCaptor<WalletTransactionJpaEntity> captor =
        ArgumentCaptor.forClass(WalletTransactionJpaEntity.class);
    verify(transactionRepo).save(captor.capture());
    WalletTransactionJpaEntity captured = captor.getValue();

    assertThat(captured.getId()).isEqualTo(transactionId);
    assertThat(captured.getWallet()).isSameAs(walletEntity);
    assertThat(captured.getReferenceId()).isEqualTo(referenceId);
    assertThat(captured.getType()).isEqualTo(TransactionType.DEPOSIT);
    assertThat(captured.getAmount()).isEqualByComparingTo(transaction.getAmount());

    assertThat(result.getId()).isEqualTo(transactionId);
    assertThat(result.getWalletId()).isEqualTo(walletId);
    assertThat(result.getType()).isEqualTo(TransactionType.DEPOSIT);
  }

  @Test
  void saveTransaction_walletNotFound_throwsIllegalArgumentException() {
    // arrange
    UUID walletId = UUID.randomUUID();
    WalletTransaction transaction = buildTransaction(UUID.randomUUID(), walletId, null);
    when(walletRepo.findById(walletId)).thenReturn(Optional.empty());

    // act & assert
    assertThatThrownBy(() -> adapter.saveTransaction(transaction))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(walletId.toString());
  }

  // --- findTransactionsByWalletId ---

  @Test
  void findTransactionsByWalletId_delegatesWithCorrectArgumentAndMapsResults() {
    // arrange
    UUID walletId = UUID.randomUUID();
    WalletJpaEntity walletEntity = buildEntity(walletId, UUID.randomUUID());
    WalletTransactionJpaEntity txEntity =
        buildTransactionEntity(UUID.randomUUID(), walletEntity, null);
    when(transactionRepo.findByWallet_IdOrderByCreatedAtDesc(walletId))
        .thenReturn(List.of(txEntity));

    // act
    List<WalletTransaction> result = adapter.findTransactionsByWalletId(walletId);

    // assert
    verify(transactionRepo).findByWallet_IdOrderByCreatedAtDesc(eq(walletId));
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getWalletId()).isEqualTo(walletId);
  }

  @Test
  void findTransactionsByWalletId_noTransactions_returnsEmptyList() {
    // arrange
    UUID walletId = UUID.randomUUID();
    when(transactionRepo.findByWallet_IdOrderByCreatedAtDesc(walletId)).thenReturn(List.of());

    // act
    List<WalletTransaction> result = adapter.findTransactionsByWalletId(walletId);

    // assert
    assertThat(result).isEmpty();
  }
}
