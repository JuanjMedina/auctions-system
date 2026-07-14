package adapter.messaging;

import domain.outbox.EventPublisher;
import domain.outbox.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Implementación in-process del puerto EventPublisher usando los eventos de Spring. Para migrar a
 * un broker (Kafka, RabbitMQ...) basta con sustituir este adaptador; el outbox, los casos de uso y
 * el dominio no cambian.
 */
@Component
@RequiredArgsConstructor
public class SpringEventPublisher implements EventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  @Override
  public void publish(OutboxEvent event) {
    applicationEventPublisher.publishEvent(
        new IntegrationEvent(
            event.getId(),
            event.getAggregateType().name(),
            event.getAggregateId(),
            event.getEventType().name(),
            event.getPayload(),
            event.getCreatedAt()));
  }
}
