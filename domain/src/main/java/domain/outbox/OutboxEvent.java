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
      Instant createdAt,
      Instant processedAt) {
    return OutboxEvent.builder()
        .id(id)
        .aggregateType(aggregateType)
        .aggregateId(aggregateId)
        .eventType(eventType)
        .payload(payload)
        .processed(processed)
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
}
