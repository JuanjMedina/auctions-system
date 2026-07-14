package adapter.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Consumidor de referencia: deja traza de cada evento de integración publicado. Aquí es donde se
 * engancharían consumidores reales (notificaciones, proyecciones, métricas...).
 */
@Slf4j
@Component
public class IntegrationEventLogger {

  @Async("publisherEventsExecutor")
  @EventListener
  public void on(IntegrationEvent event) {
    log.info(
        "Integration event received: type={} aggregate={}/{} payload={}",
        event.eventType(),
        event.aggregateType(),
        event.aggregateId(),
        event.payload());
  }
}
