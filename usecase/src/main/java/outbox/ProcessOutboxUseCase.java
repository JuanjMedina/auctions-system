package outbox;

import domain.outbox.EventPublisher;
import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import outbox.input.ProcessOutboxInput;
import outbox.output.ProcessOutboxResult;
import shared.UseCase;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessOutboxUseCase implements UseCase<ProcessOutboxInput, ProcessOutboxResult> {

  static final int MAX_RETRIES = 5;

  private final OutboxEventRepository outboxEventRepository;
  private final EventPublisher eventPublisher;
  private final TransactionTemplate transactionTemplate;

  @Override
  public ProcessOutboxResult execute(ProcessOutboxInput input) {
    List<OutboxEvent> batch = outboxEventRepository.findUnprocessed(MAX_RETRIES, input.batchSize());

    int processed = 0;
    int failed = 0;

    // Cada evento se publica y se marca en su propia transacción: un fallo
    // no revierte los eventos ya publicados ni bloquea el resto del batch.
    for (OutboxEvent event : batch) {
      if (publishEvent(event)) {
        processed++;
      } else {
        failed++;
      }
    }

    return new ProcessOutboxResult(processed, failed);
  }

  private boolean publishEvent(OutboxEvent event) {
    try {
      transactionTemplate.executeWithoutResult(
          tx -> {
            eventPublisher.publish(event);
            event.markAsProcessed();
            outboxEventRepository.save(event);
          });
      log.info(
          "Outbox event published: type={} aggregateId={}",
          event.getEventType(),
          event.getAggregateId());
      return true;
    } catch (Exception e) {
      registerFailure(event, e);
      return false;
    }
  }

  private void registerFailure(OutboxEvent event, Exception cause) {
    try {
      transactionTemplate.executeWithoutResult(
          tx -> {
            event.markAsFailed(cause.getMessage());
            outboxEventRepository.save(event);
          });
      if (event.hasExhaustedRetries(MAX_RETRIES)) {
        log.error(
            "Outbox event exhausted retries and will be skipped: id={} type={} error={}",
            event.getId(),
            event.getEventType(),
            cause.getMessage());
      } else {
        log.warn(
            "Outbox event failed (retry {}/{}): id={} type={} error={}",
            event.getRetryCount(),
            MAX_RETRIES,
            event.getId(),
            event.getEventType(),
            cause.getMessage());
      }
    } catch (Exception persistError) {
      log.error(
          "Could not register outbox failure: id={} error={}",
          event.getId(),
          persistError.getMessage());
    }
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
