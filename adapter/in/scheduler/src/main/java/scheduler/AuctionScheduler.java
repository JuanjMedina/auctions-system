package scheduler;

import auction.ActivateScheduledAuctionsUseCase;
import auction.ProcessExpiredAuctionsUseCase;
import auction.output.ProcessAuctionsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shared.NoInput;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

  private static final int DELAY_MS = 30_000; // 30 segundos

  private final ProcessExpiredAuctionsUseCase processExpiredAuctionsUseCase;
  private final ActivateScheduledAuctionsUseCase activateScheduledAuctionsUseCase;

  @Scheduled(fixedDelay = DELAY_MS)
  public void closeExpiredAuctions() {
    ProcessAuctionsResult result = processExpiredAuctionsUseCase.run(NoInput.INSTANCE);
    if (result.processed() > 0 || result.failed() > 0) {
      log.info(
          "Scheduler closeExpired: cerradas={} errores={}", result.processed(), result.failed());
    }
  }

  @Scheduled(fixedDelay = DELAY_MS)
  public void activateScheduledAuctions() {
    ProcessAuctionsResult result = activateScheduledAuctionsUseCase.run(NoInput.INSTANCE);
    if (result.processed() > 0 || result.failed() > 0) {
      log.info(
          "Scheduler activateScheduled: activadas={} errores={}",
          result.processed(),
          result.failed());
    }
  }
}
