package scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import outbox.ProcessOutboxUseCase;
import outbox.input.ProcessOutboxInput;
import outbox.output.ProcessOutboxResult;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

  private final ProcessOutboxUseCase processOutboxUseCase;

  @Scheduled(fixedDelay = 5000)
  public void poll() {
    ProcessOutboxResult result = processOutboxUseCase.run(ProcessOutboxInput.DEFAULT);

    if (result.processed() > 0 || result.failed() > 0) {
      log.info(
          "Outbox poll completed: processed={} failed={}", result.processed(), result.failed());
    }
  }
}
