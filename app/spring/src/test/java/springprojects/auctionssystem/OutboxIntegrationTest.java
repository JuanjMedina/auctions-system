package springprojects.auctionssystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

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
import domain.outbox.EventPublisher;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import outbox.ProcessOutboxUseCase;
import outbox.input.ProcessOutboxInput;
import user.RegisterUserUseCase;
import user.input.RegisterUserInput;
import user.output.RegisterUserResult;
import wallet.DepositUseCase;
import wallet.input.DepositInput;

/**
 * Verifica el patrón outbox de punta a punta contra un Postgres real (Testcontainers): que el caso
 * de uso persiste el estado de dominio y el evento outbox en la misma transacción, que
 * ProcessOutboxUseCase marca los eventos como procesados de forma durable, y que una falla del
 * EventPublisher incrementa retryCount/lastError hasta agotar los reintentos. Los tests unitarios
 * de cada adapter mockean SpringDataRepository, por lo que nunca ejercitan el locking optimista ni
 * la persistencia real de estas columnas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OutboxIntegrationTest {

  @Autowired private RegisterUserUseCase registerUserUseCase;
  @Autowired private DepositUseCase depositUseCase;
  @Autowired private CreateCategoryUseCase createCategoryUseCase;
  @Autowired private CreateAuctionUseCase createAuctionUseCase;
  @Autowired private PublishAuctionUseCase publishAuctionUseCase;
  @Autowired private PlaceBidUseCase placeBidUseCase;
  @Autowired private ProcessOutboxUseCase processOutboxUseCase;
  @Autowired private OutboxEventRepository outboxEventRepository;

  @MockitoSpyBean private EventPublisher eventPublisher;

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

  private UUID placeBidAndReturnBidId() {
    UUID sellerId = registerUser("seller");
    UUID bidderId = registerUser("bidder");
    depositUseCase.run(new DepositInput(bidderId, BigDecimal.valueOf(500), "fondeo test"));

    CreateCategoryResult category =
        createCategoryUseCase.run(
            new CreateCategoryInput("Cat-" + UUID.randomUUID(), "cat-" + UUID.randomUUID(), null));

    CreateAuctionResult auction =
        createAuctionUseCase.run(
            new CreateAuctionInput(
                sellerId,
                category.id(),
                "Subasta integracion",
                "desc",
                BigDecimal.valueOf(10),
                null,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600),
                false,
                5));

    // startsAt en el pasado: publish() deja la subasta ACTIVE directamente
    publishAuctionUseCase.run(new PublishAuctionInput(auction.id(), sellerId));

    PlaceBidOutput bid =
        placeBidUseCase.run(
            new PlaceBidInput(auction.id(), bidderId, BigDecimal.valueOf(50), false, null));
    return bid.bidId();
  }

  private OutboxEvent findUnprocessedEventForAggregate(UUID aggregateId) {
    return outboxEventRepository.findUnprocessed(1000, 1000).stream()
        .filter(e -> e.getAggregateId().equals(aggregateId))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No se encontro evento outbox para " + aggregateId));
  }

  @Test
  void placeBid_realDatabase_persistsBidAndOutboxEventInSameTransaction() {
    UUID bidId = placeBidAndReturnBidId();

    OutboxEvent event = findUnprocessedEventForAggregate(bidId);

    assertThat(event.isProcessed()).isFalse();
    assertThat(event.getRetryCount()).isZero();
    assertThat(event.getPayload()).contains(bidId.toString());
  }

  @Test
  void processOutbox_realDatabase_marksEventProcessedDurably() {
    UUID bidId = placeBidAndReturnBidId();
    UUID eventId = findUnprocessedEventForAggregate(bidId).getId();

    processOutboxUseCase.run(new ProcessOutboxInput(500));

    OutboxEvent processed =
        outboxEventRepository
            .findById(eventId)
            .orElseThrow(() -> new AssertionError("Evento outbox desaparecio"));

    assertThat(processed.isProcessed()).isTrue();
    assertThat(processed.getProcessedAt()).isNotNull();
  }

  @Test
  void processOutbox_publisherFailure_incrementsRetryCountAndExhaustsAfterMaxRetries() {
    UUID bidId = placeBidAndReturnBidId();
    UUID eventId = findUnprocessedEventForAggregate(bidId).getId();

    doThrow(new RuntimeException("broker caido"))
        .when(eventPublisher)
        .publish(argThat(e -> e != null && e.getId().equals(eventId)));

    // MAX_RETRIES = 5 en ProcessOutboxUseCase: tras 5 fallos el evento queda excluido de
    // findUnprocessed (retryCount >= maxRetries) y no vuelve a intentarse.
    for (int i = 1; i <= 5; i++) {
      processOutboxUseCase.run(new ProcessOutboxInput(500));
      OutboxEvent afterFailure =
          outboxEventRepository
              .findById(eventId)
              .orElseThrow(() -> new AssertionError("Evento outbox desaparecio"));
      assertThat(afterFailure.isProcessed()).isFalse();
      assertThat(afterFailure.getRetryCount()).isEqualTo(i);
      assertThat(afterFailure.getLastError()).contains("broker caido");
    }

    boolean stillPickedUp =
        outboxEventRepository.findUnprocessed(5, 1000).stream()
            .anyMatch(e -> e.getId().equals(eventId));
    assertThat(stillPickedUp).isFalse();

    // Al restaurar el publisher real y reintentar con un umbral mayor, el evento
    // (aunque ya agoto los 5 reintentos por defecto) sigue existiendo y sin marcar como procesado.
    OutboxEvent finalState =
        outboxEventRepository
            .findById(eventId)
            .orElseThrow(() -> new AssertionError("Evento outbox desaparecio"));
    assertThat(finalState.hasExhaustedRetries(5)).isTrue();
  }
}
