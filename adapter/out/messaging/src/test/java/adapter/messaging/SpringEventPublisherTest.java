package adapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import domain.outbox.AggregateType;
import domain.outbox.EventType;
import domain.outbox.OutboxEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SpringEventPublisherTest {

  @Mock private ApplicationEventPublisher applicationEventPublisher;

  @InjectMocks private SpringEventPublisher publisher;

  @Test
  void publish_mapsOutboxEventToIntegrationEvent() {
    // arrange
    UUID aggregateId = UUID.randomUUID();
    OutboxEvent event =
        OutboxEvent.create(AggregateType.BID, aggregateId, EventType.BID_PLACED, "{\"a\":1}");

    // act
    publisher.publish(event);

    // assert
    ArgumentCaptor<IntegrationEvent> captor = ArgumentCaptor.forClass(IntegrationEvent.class);
    verify(applicationEventPublisher).publishEvent(captor.capture());
    IntegrationEvent published = captor.getValue();
    assertThat(published.eventId()).isEqualTo(event.getId());
    assertThat(published.aggregateType()).isEqualTo("BID");
    assertThat(published.aggregateId()).isEqualTo(aggregateId);
    assertThat(published.eventType()).isEqualTo("BID_PLACED");
    assertThat(published.payload()).isEqualTo("{\"a\":1}");
    assertThat(published.occurredAt()).isEqualTo(event.getCreatedAt());
  }
}
