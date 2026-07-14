package adapter.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.outbox.AggregateType;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import entity.outbox.OutboxEventJpaEntity;
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
import org.springframework.data.domain.PageRequest;
import repository.outbox.SpringDataOutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class OutboxEventJpaAdapterTest {

  @Mock private SpringDataOutboxEventRepository springDataRepo;

  private OutboxEventJpaAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new OutboxEventJpaAdapter(springDataRepo);
  }

  private OutboxEvent buildEvent(UUID id, boolean processed) {
    return OutboxEvent.reconstitute(
        id,
        AggregateType.AUCTION,
        UUID.randomUUID(),
        EventType.BID_PLACED,
        "{\"key\":\"value\"}",
        processed,
        0,
        null,
        Instant.now(),
        processed ? Instant.now() : null);
  }

  private OutboxEventJpaEntity buildEntity(UUID id, boolean processed) {
    return OutboxEventJpaEntity.builder()
        .id(id)
        .aggregateType(AggregateType.AUCTION)
        .aggregateId(UUID.randomUUID())
        .eventType(EventType.BID_PLACED.name())
        .payload("{\"key\":\"value\"}")
        .processed(processed)
        .createdAt(Instant.now())
        .processedAt(processed ? Instant.now() : null)
        .build();
  }

  // --- save ---

  @Test
  void save_unprocessedEvent_delegatesAndMapsResult() {
    // arrange
    UUID id = UUID.randomUUID();
    OutboxEvent event = buildEvent(id, false);
    OutboxEventJpaEntity savedEntity = buildEntity(id, false);

    when(springDataRepo.save(any(OutboxEventJpaEntity.class))).thenReturn(savedEntity);

    // act
    OutboxEvent result = adapter.save(event);

    // assert
    ArgumentCaptor<OutboxEventJpaEntity> captor =
        ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
    verify(springDataRepo).save(captor.capture());
    OutboxEventJpaEntity captured = captor.getValue();

    assertThat(captured.getId()).isEqualTo(id);
    assertThat(captured.getAggregateType()).isEqualTo(AggregateType.AUCTION);
    assertThat(captured.getAggregateId()).isEqualTo(event.getAggregateId());
    assertThat(captured.getEventType()).isEqualTo("BID_PLACED");
    assertThat(captured.getPayload()).isEqualTo(event.getPayload());
    assertThat(captured.isProcessed()).isFalse();

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getEventType()).isEqualTo(EventType.BID_PLACED);
    assertThat(result.isProcessed()).isFalse();
  }

  // --- findById ---

  @Test
  void findById_existingEvent_returnsMappedDomainWithParsedEventType() {
    // arrange
    UUID id = UUID.randomUUID();
    OutboxEventJpaEntity entity = buildEntity(id, true);
    when(springDataRepo.findById(id)).thenReturn(Optional.of(entity));

    // act
    Optional<OutboxEvent> result = adapter.findById(id);

    // assert
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(id);
    assertThat(result.get().getEventType()).isEqualTo(EventType.BID_PLACED);
    assertThat(result.get().isProcessed()).isTrue();
    assertThat(result.get().getProcessedAt()).isNotNull();
  }

  @Test
  void findById_missingEvent_returnsEmptyOptional() {
    // arrange
    UUID id = UUID.randomUUID();
    when(springDataRepo.findById(id)).thenReturn(Optional.empty());

    // act
    Optional<OutboxEvent> result = adapter.findById(id);

    // assert
    assertThat(result).isEmpty();
  }

  // --- findUnprocessed ---

  @Test
  void findUnprocessed_delegatesWithMaxRetriesAndLimitAndMapsResults() {
    // arrange
    OutboxEventJpaEntity entity = buildEntity(UUID.randomUUID(), false);
    when(springDataRepo.findByProcessedFalseAndRetryCountLessThanOrderByCreatedAtAsc(
            5, PageRequest.of(0, 10)))
        .thenReturn(List.of(entity));

    // act
    List<OutboxEvent> result = adapter.findUnprocessed(5, 10);

    // assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).isProcessed()).isFalse();
  }

  @Test
  void findUnprocessed_noneUnprocessed_returnsEmptyList() {
    // arrange
    when(springDataRepo.findByProcessedFalseAndRetryCountLessThanOrderByCreatedAtAsc(
            any(Integer.class), any(PageRequest.class)))
        .thenReturn(List.of());

    // act
    List<OutboxEvent> result = adapter.findUnprocessed(5, 10);

    // assert
    assertThat(result).isEmpty();
  }

  // --- saveAll ---

  @Test
  void saveAll_multipleEvents_delegatesWithMappedEntities() {
    // arrange
    OutboxEvent event1 = buildEvent(UUID.randomUUID(), false);
    OutboxEvent event2 = buildEvent(UUID.randomUUID(), true);

    // act
    adapter.saveAll(List.of(event1, event2));

    // assert
    ArgumentCaptor<List<OutboxEventJpaEntity>> captor = ArgumentCaptor.forClass(List.class);
    verify(springDataRepo).saveAll(captor.capture());
    List<OutboxEventJpaEntity> captured = captor.getValue();

    assertThat(captured).hasSize(2);
    assertThat(captured.get(0).getId()).isEqualTo(event1.getId());
    assertThat(captured.get(1).getId()).isEqualTo(event2.getId());
    assertThat(captured.get(1).isProcessed()).isTrue();
  }

  @Test
  void saveAll_emptyList_delegatesWithEmptyList() {
    // act
    adapter.saveAll(List.of());

    // assert
    ArgumentCaptor<List<OutboxEventJpaEntity>> captor = ArgumentCaptor.forClass(List.class);
    verify(springDataRepo).saveAll(captor.capture());
    assertThat(captor.getValue()).isEmpty();
  }
}
