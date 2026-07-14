package domain.outbox;

/**
 * Puerto de salida para publicar eventos de dominio ya persistidos en el outbox. Una excepción
 * señala que la publicación falló y el evento debe reintentarse.
 */
public interface EventPublisher {

  void publish(OutboxEvent event);
}
