package outbox;

import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import outbox.input.ProcessOutboxInput;
import outbox.output.ProcessOutboxResult;
import shared.UseCase;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessOutboxUseCase implements UseCase<ProcessOutboxInput, ProcessOutboxResult> {

  private final OutboxEventRepository outboxEventRepository;

  @Override
  @Transactional
  public ProcessOutboxResult execute(ProcessOutboxInput input) {
    List<OutboxEvent> pending = outboxEventRepository.findUnprocessed();

    List<OutboxEvent> batch = pending.stream().limit(input.batchSize()).toList();

    int processed = 0;
    int failed = 0;

    for (OutboxEvent event : batch) {
      try {
        event.markAsProcessed();
        outboxEventRepository.save(event);
        log.info(
            "Outbox event processed: type={} aggregateId={}",
            event.getEventType(),
            event.getAggregateId());
        processed++;
      } catch (Exception e) {
        log.error(
            "Failed to process outbox event: id={} type={} error={}",
            event.getId(),
            event.getEventType(),
            e.getMessage());
        failed++;
      }
    }

    return new ProcessOutboxResult(processed, failed);
  }

  @Override
  public ProcessOutboxResult failed(Exception exception) {
    log.error("Outbox processing batch failed: {}", exception.getMessage());
    return UseCase.super.failed(exception);
  }

  @Override
  public String errorMessage() {
    return "Error al procesar el outbox";
  }
}
