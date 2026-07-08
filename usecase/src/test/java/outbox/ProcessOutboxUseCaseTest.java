package outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.outbox.AggregateType;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import outbox.input.ProcessOutboxInput;
import outbox.output.ProcessOutboxResult;

@ExtendWith(MockitoExtension.class)
class ProcessOutboxUseCaseTest {

  @Mock private OutboxEventRepository outboxEventRepository;

  @InjectMocks private ProcessOutboxUseCase useCase;

  // --- fixtures ---

  private OutboxEvent buildEvent() {
    return OutboxEvent.create(
        AggregateType.BID, UUID.randomUUID(), EventType.BID_PLACED, "{\"foo\":\"bar\"}");
  }

  // --- happy path ---

  @Test
  void execute_pendingEvents_processesAllWithinBatchSize() {
    // arrange
    List<OutboxEvent> pending = List.of(buildEvent(), buildEvent(), buildEvent());
    when(outboxEventRepository.findUnprocessed()).thenReturn(pending);
    when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    ProcessOutboxResult result = useCase.run(new ProcessOutboxInput(50));

    // assert
    assertThat(result.processed()).isEqualTo(3);
    assertThat(result.failed()).isZero();
    verify(outboxEventRepository, times(3)).save(any());
  }

  @Test
  void execute_pendingEvents_marksEventsAsProcessed() {
    // arrange
    OutboxEvent event = buildEvent();
    when(outboxEventRepository.findUnprocessed()).thenReturn(List.of(event));
    when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    useCase.run(new ProcessOutboxInput(50));

    // assert
    assertThat(event.isProcessed()).isTrue();
    verify(outboxEventRepository).save(event);
  }

  // --- respeta el batch size ---

  @Test
  void execute_moreEventsThanBatchSize_onlyProcessesBatchSizeAmount() {
    // arrange
    List<OutboxEvent> pending = List.of(buildEvent(), buildEvent(), buildEvent(), buildEvent());
    when(outboxEventRepository.findUnprocessed()).thenReturn(pending);
    when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    ProcessOutboxResult result = useCase.run(new ProcessOutboxInput(2));

    // assert
    assertThat(result.processed()).isEqualTo(2);
    verify(outboxEventRepository, times(2)).save(any());
  }

  // --- lista vacía ---

  @Test
  void execute_noPendingEvents_returnsZeroProcessedAndZeroFailed() {
    // arrange
    when(outboxEventRepository.findUnprocessed()).thenReturn(List.of());

    // act
    ProcessOutboxResult result = useCase.run(ProcessOutboxInput.DEFAULT);

    // assert
    assertThat(result.processed()).isZero();
    assertThat(result.failed()).isZero();
    verify(outboxEventRepository, never()).save(any());
  }

  // --- error al procesar un evento individual ---

  @Test
  void execute_eventAlreadyProcessed_countsAsFailedWithoutThrowing() {
    // arrange: an already-processed event throws IllegalStateException on markAsProcessed()
    OutboxEvent alreadyProcessed = buildEvent();
    alreadyProcessed.markAsProcessed();
    OutboxEvent normalEvent = buildEvent();

    when(outboxEventRepository.findUnprocessed())
        .thenReturn(List.of(alreadyProcessed, normalEvent));
    when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // act
    ProcessOutboxResult result = useCase.run(new ProcessOutboxInput(50));

    // assert: batch processing catches per-event errors, so the use case does not throw
    assertThat(result.processed()).isEqualTo(1);
    assertThat(result.failed()).isEqualTo(1);
    verify(outboxEventRepository, times(1)).save(normalEvent);
  }
}
