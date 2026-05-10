package adapter.out.persistence.outbox;

import domain.outbox.OutboxEvent;
import domain.outbox.OutboxEventRepository;
import entity.outbox.OutboxEventJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import repository.outbox.SpringDataOutboxEventRepository;

@Component
@RequiredArgsConstructor
public class OutboxEventJpaAdapter implements OutboxEventRepository {

  private final SpringDataOutboxEventRepository springDataRepo;

  @Override
  public OutboxEvent save(OutboxEvent event) {
    return toDomain(springDataRepo.save(toJpaEntity(event)));
  }

  @Override
  public Optional<OutboxEvent> findById(UUID id) {
    return springDataRepo.findById(id).map(this::toDomain);
  }

  @Override
  public List<OutboxEvent> findUnprocessed() {
    return springDataRepo.findByProcessedFalseOrderByCreatedAtAsc().stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public void saveAll(List<OutboxEvent> events) {
    springDataRepo.saveAll(events.stream().map(this::toJpaEntity).toList());
  }

  private OutboxEventJpaEntity toJpaEntity(OutboxEvent event) {
    return OutboxEventJpaEntity.builder()
        .id(event.getId())
        .aggregateType(event.getAggregateType())
        .aggregateId(event.getAggregateId())
        .eventType(event.getEventType())
        .payload(event.getPayload())
        .processed(event.isProcessed())
        .processedAt(event.getProcessedAt())
        .build();
  }

  private OutboxEvent toDomain(OutboxEventJpaEntity entity) {
    return OutboxEvent.reconstitute(
        entity.getId(),
        entity.getAggregateType(),
        entity.getAggregateId(),
        entity.getEventType(),
        entity.getPayload(),
        entity.isProcessed(),
        entity.getCreatedAt(),
        entity.getProcessedAt());
  }
}
