package domain.outbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {
  OutboxEvent save(OutboxEvent event);

  Optional<OutboxEvent> findById(UUID id);

  List<OutboxEvent> findUnprocessed();

  void saveAll(List<OutboxEvent> events);
}
