package domain.outbox;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
public class OutboxEvent {

  private final UUID id;
  private final AggregateType aggregateType;
  private final UUID aggregateId;
  private final String eventType; // e.g. "AUCTION_CLOSED", "BID_PLACED"
  private final String payload; // JSON serializado
  private boolean processed;
  private final Instant createdAt;
  private Instant processedAt;

  private OutboxEvent(
      UUID id,
      AggregateType aggregateType,
      UUID aggregateId,
      String eventType,
      String payload,
      boolean processed,
      Instant createdAt,
      Instant processedAt) {
    this.id = id;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.payload = payload;
    this.processed = processed;
    this.createdAt = createdAt;
    this.processedAt = processedAt;
  }

  public static OutboxEvent create(
      AggregateType aggregateType, UUID aggregateId, String eventType, String payload) {
    return new OutboxEvent(
        UUID.randomUUID(),
        aggregateType,
        aggregateId,
        eventType,
        payload,
        false,
        Instant.now(),
        null);
  }

  public static OutboxEvent reconstitute(
      UUID id,
      AggregateType aggregateType,
      UUID aggregateId,
      String eventType,
      String payload,
      boolean processed,
      Instant createdAt,
      Instant processedAt) {
    return new OutboxEvent(
        id, aggregateType, aggregateId, eventType, payload, processed, createdAt, processedAt);
  }

  public void markAsProcessed() {
    if (processed) {
      throw new IllegalStateException("OutboxEvent " + id + " already processed");
    }
    this.processed = true;
    this.processedAt = Instant.now();
  }
}
