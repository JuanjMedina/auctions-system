package springprojects.auctionssystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;

import auction.CreateAuctionUseCase;
import auction.PublishAuctionUseCase;
import auction.input.CreateAuctionInput;
import auction.input.PublishAuctionInput;
import auction.output.CreateAuctionResult;
import bid.PlaceBidUseCase;
import bid.input.PlaceBidInput;
import bid.output.PlaceBidOutput;
import category.CreateCategoryUseCase;
import category.input.CreateCategoryInput;
import category.output.CreateCategoryResult;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import domain.shared.ConcurrencyException;
import domain.wallets.Wallet;
import domain.wallets.WalletRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import user.RegisterUserUseCase;
import user.input.RegisterUserInput;
import user.output.RegisterUserResult;
import wallet.DepositUseCase;
import wallet.input.DepositInput;

/**
 * Ejercita el locking optimista (columna @Version) contra un Postgres real. Los tests unitarios de
 * los adapters (p.ej. AuctionJpaAdapterTest) mockean SpringDataRepository y simplemente asumen que
 * OptimisticLockingFailureException puede ocurrir; aquí se provoca un conflicto de versión de
 * verdad y se comprueba tanto la traducción a ConcurrencyException como la recuperación
 * via @Retryable en PlaceBidUseCase.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConcurrencyIntegrationTest {

  @Autowired private RegisterUserUseCase registerUserUseCase;
  @Autowired private DepositUseCase depositUseCase;
  @Autowired private CreateCategoryUseCase createCategoryUseCase;
  @Autowired private CreateAuctionUseCase createAuctionUseCase;
  @Autowired private PublishAuctionUseCase publishAuctionUseCase;
  @Autowired private PlaceBidUseCase placeBidUseCase;
  @Autowired private WalletRepository walletRepository;

  @MockitoSpyBean private AuctionRepository auctionRepository;

  private UUID registerUser(String prefix) {
    RegisterUserResult result =
        registerUserUseCase.run(
            new RegisterUserInput(
                prefix + "-" + UUID.randomUUID() + "@test.com",
                prefix + UUID.randomUUID().toString().substring(0, 8),
                "Password123!",
                "Test " + prefix,
                null,
                "BUYER"));
    return result.userId();
  }

  private UUID createActiveAuction(UUID sellerId) {
    CreateCategoryResult category =
        createCategoryUseCase.run(
            new CreateCategoryInput("Cat-" + UUID.randomUUID(), "cat-" + UUID.randomUUID(), null));

    CreateAuctionResult auction =
        createAuctionUseCase.run(
            new CreateAuctionInput(
                sellerId,
                category.id(),
                "Subasta concurrencia",
                "desc",
                BigDecimal.valueOf(10),
                null,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600),
                false,
                5));

    publishAuctionUseCase.run(new PublishAuctionInput(auction.id(), sellerId));
    return auction.id();
  }

  @Test
  void walletSave_staleVersion_throwsConcurrencyExceptionAgainstRealDatabase() {
    UUID userId = registerUser("wallet-owner");

    Wallet staleCopy1 = walletRepository.getByUserId(userId);
    Wallet staleCopy2 = walletRepository.getByUserId(userId);

    staleCopy1.deposit(BigDecimal.TEN, "deposito 1");
    walletRepository.save(staleCopy1);

    staleCopy2.deposit(BigDecimal.valueOf(20), "deposito 2");
    assertThatThrownBy(() -> walletRepository.save(staleCopy2))
        .isInstanceOf(ConcurrencyException.class);

    // El unico deposito que quedo aplicado en la base real es el de la copia que gano la carrera.
    Wallet finalState = walletRepository.getByUserId(userId);
    assertThat(finalState.getBalance()).isEqualByComparingTo(BigDecimal.TEN);
  }

  @Test
  void placeBid_afterSimulatedVersionConflict_retryableRecoversAndPersistsBid() {
    UUID sellerId = registerUser("seller");
    UUID bidderId = registerUser("bidder");
    depositUseCase.run(new DepositInput(bidderId, BigDecimal.valueOf(500), "fondeo test"));
    UUID auctionId = createActiveAuction(sellerId);

    // Simula que, entre leer y guardar la subasta, otro proceso ya la actualizo (conflicto de
    // version real detectado por Postgres en un escenario concurrente). Solo la primera
    // invocacion falla; @Retryable en PlaceBidUseCase debe reintentar el caso de uso completo
    // (releyendo la subasta) y la segunda invocacion persiste contra la base real.
    AtomicInteger callCount = new AtomicInteger();
    doAnswer(
            invocation -> {
              if (callCount.getAndIncrement() == 0) {
                throw new ConcurrencyException("Auction", auctionId);
              }
              return invocation.callRealMethod();
            })
        .when(auctionRepository)
        .save(argThat(a -> a != null && a.getId().equals(auctionId)));

    PlaceBidOutput bid =
        placeBidUseCase.run(
            new PlaceBidInput(auctionId, bidderId, BigDecimal.valueOf(50), false, null));

    assertThat(callCount.get()).isEqualTo(2);
    assertThat(bid.amount()).isEqualByComparingTo(BigDecimal.valueOf(50));

    Auction persisted = auctionRepository.getById(auctionId);
    assertThat(persisted.getCurrentPrice()).isEqualByComparingTo(BigDecimal.valueOf(50));
    assertThat(persisted.getCurrentWinnerId()).isEqualTo(bidderId);
  }
}
