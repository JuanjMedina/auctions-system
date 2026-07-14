package domain.outbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {
  OutboxEvent save(OutboxEvent event);

  Optional<OutboxEvent> findById(UUID id);

  /** Eventos pendientes (no procesados y con reintentos disponibles), más antiguos primero. */
  List<OutboxEvent> findUnprocessed(int maxRetries, int limit);

  void saveAll(List<OutboxEvent> events);
}
