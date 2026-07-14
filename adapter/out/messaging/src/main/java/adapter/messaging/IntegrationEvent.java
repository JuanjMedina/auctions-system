package adapter.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje de integración que viaja hacia los consumidores. Es la representación "externa" de un
 * OutboxEvent: tipos planos, sin clases de dominio, para que un futuro transporte (Kafka,
 * RabbitMQ...) pueda serializarlo tal cual.
 */
public record IntegrationEvent(
    UUID eventId,
    String aggregateType,
    UUID aggregateId,
    String eventType,
    String payload,
    Instant occurredAt) {}
