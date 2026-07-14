package repository.outbox;

import entity.outbox.OutboxEventJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

  List<OutboxEventJpaEntity> findByProcessedFalseAndRetryCountLessThanOrderByCreatedAtAsc(
      int maxRetries, Pageable pageable);
}
