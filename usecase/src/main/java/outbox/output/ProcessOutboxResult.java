package outbox.output;

public record ProcessOutboxResult(int processed, int failed) {}
