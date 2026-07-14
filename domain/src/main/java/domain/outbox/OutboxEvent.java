package domain.outbox;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = lombok.AccessLevel.PRIVATE)
public class OutboxEvent {

  private final UUID id;
  private final AggregateType aggregateType;
  private final UUID aggregateId;
  private final EventType eventType;
  private final String payload;
  private boolean processed;
  private int retryCount;
  private String lastError;
  private final Instant createdAt;
  private Instant processedAt;

  public static OutboxEvent create(
      AggregateType aggregateType, UUID aggregateId, EventType eventType, String payload) {
    return OutboxEvent.builder()
        .id(UUID.randomUUID())
        .aggregateType(aggregateType)
        .aggregateId(aggregateId)
        .eventType(eventType)
        .payload(payload)
        .processed(false)
        .retryCount(0)
        .lastError(null)
        .createdAt(Instant.now())
        .processedAt(null)
        .build();
  }

  public static OutboxEvent reconstitute(
      UUID id,
      AggregateType aggregateType,
      UUID aggregateId,
      EventType eventType,
      String payload,
      boolean processed,
      int retryCount,
      String lastError,
      Instant createdAt,
      Instant processedAt) {
    return OutboxEvent.builder()
        .id(id)
        .aggregateType(aggregateType)
        .aggregateId(aggregateId)
        .eventType(eventType)
        .payload(payload)
        .processed(processed)
        .retryCount(retryCount)
        .lastError(lastError)
        .createdAt(createdAt)
        .processedAt(processedAt)
        .build();
  }

  public void markAsProcessed() {
    if (processed) {
      throw new IllegalStateException("OutboxEvent " + id + " already processed");
    }
    this.processed = true;
    this.processedAt = Instant.now();
  }

  /** Registra un intento de publicación fallido; el evento queda pendiente para reintento. */
  public void markAsFailed(String error) {
    if (processed) {
      throw new IllegalStateException("OutboxEvent " + id + " already processed");
    }
    this.retryCount++;
    this.lastError = error;
  }

  public boolean hasExhaustedRetries(int maxRetries) {
    return retryCount >= maxRetries;
  }
}
