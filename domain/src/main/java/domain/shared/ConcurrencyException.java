package domain.shared;

/**
 * Lanzada cuando una operación falla por conflicto de escritura concurrente (optimistic locking).
 */
public class ConcurrencyException extends RuntimeException {

  public ConcurrencyException(String entity, Object id) {
    super(String.format("Conflicto de concurrencia al guardar %s con id %s", entity, id));
  }
}
