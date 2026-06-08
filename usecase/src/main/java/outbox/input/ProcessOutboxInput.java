package outbox.input;

public record ProcessOutboxInput(int batchSize) {

  public static final ProcessOutboxInput DEFAULT = new ProcessOutboxInput(50);
}
