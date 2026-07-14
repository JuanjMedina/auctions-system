package outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.outbox.AggregateType;
import domain.outbox.EventPublisher;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import outbox.input.ProcessOutboxInput;
import outbox.output.ProcessOutboxResult;

@ExtendWith(MockitoExtension.class)
class ProcessOutboxUseCaseTest {

  @Mock private OutboxEventRepository outboxEventRepository;
  @Mock private EventPublisher eventPublisher;
  @Mock private TransactionTemplate transactionTemplate;

  @InjectMocks private ProcessOutboxUseCase useCase;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void executeTransactionsInline() {
    lenient()
        .doAnswer(
            inv -> {
              inv.getArgument(0, Consumer.class).accept((TransactionStatus) null);
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(any());
  }

  // --- fixtures ---

  private OutboxEvent buildEvent() {
    return OutboxEvent.create(
        AggregateType.BID, UUID.randomUUID(), EventType.BID_PLACED, "{\"foo\":\"bar\"}");
  }

  // --- happy path ---

  @Test
  void execute_pendingEvents_publishesAndMarksAllAsProcessed() {
    // arrange
    List<OutboxEvent> pending = List.of(buildEvent(), buildEvent(), buildEvent());
    when(outboxEventRepository.findUnprocessed(anyInt(), eq(50))).thenReturn(pending);
    when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    ProcessOutboxResult result = useCase.run(new ProcessOutboxInput(50));

    // assert
    assertThat(result.processed()).isEqualTo(3);
    assertThat(result.failed()).isZero();
    verify(eventPublisher, times(3)).publish(any());
    verify(outboxEventRepository, times(3)).save(any());
    assertThat(pending).allMatch(OutboxEvent::isProcessed);
  }

  @Test
  void execute_batchSizeAndMaxRetries_arePassedToTheQuery() {
    // arrange
    when(outboxEventRepository.findUnprocessed(anyInt(), anyInt())).thenReturn(List.of());

    // act
    useCase.run(new ProcessOutboxInput(7));

    // assert
    verify(outboxEventRepository).findUnprocessed(ProcessOutboxUseCase.MAX_RETRIES, 7);
  }

  // --- lista vacía ---

  @Test
  void execute_noPendingEvents_returnsZeroProcessedAndZeroFailed() {
    // arrange
    when(outboxEventRepository.findUnprocessed(anyInt(), anyInt())).thenReturn(List.of());

    // act
    ProcessOutboxResult result = useCase.run(ProcessOutboxInput.DEFAULT);

    // assert
    assertThat(result.processed()).isZero();
    assertThat(result.failed()).isZero();
    verify(outboxEventRepository, never()).save(any());
  }

  // --- fallo de publicación ---

  @Test
  void execute_publisherFails_incrementsRetryCountAndKeepsEventPending() {
    // arrange
    OutboxEvent failing = buildEvent();
    when(outboxEventRepository.findUnprocessed(anyInt(), anyInt())).thenReturn(List.of(failing));
    when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    doThrow(new RuntimeException("broker down")).when(eventPublisher).publish(failing);

    // act
    ProcessOutboxResult result = useCase.run(new ProcessOutboxInput(50));

    // assert
    assertThat(result.processed()).isZero();
    assertThat(result.failed()).isEqualTo(1);
    assertThat(failing.isProcessed()).isFalse();
    assertThat(failing.getRetryCount()).isEqualTo(1);
    assertThat(failing.getLastError()).isEqualTo("broker down");
    verify(outboxEventRepository).save(failing);
  }

  @Test
  void execute_oneEventFails_othersAreStillPublished() {
    // arrange
    OutboxEvent failing = buildEvent();
    OutboxEvent healthy = buildEvent();
    when(outboxEventRepository.findUnprocessed(anyInt(), anyInt()))
        .thenReturn(List.of(failing, healthy));
    when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    doThrow(new RuntimeException("boom")).when(eventPublisher).publish(failing);

    // act
    ProcessOutboxResult result = useCase.run(new ProcessOutboxInput(50));

    // assert
    assertThat(result.processed()).isEqualTo(1);
    assertThat(result.failed()).isEqualTo(1);
    assertThat(healthy.isProcessed()).isTrue();
    assertThat(failing.isProcessed()).isFalse();
  }
}
