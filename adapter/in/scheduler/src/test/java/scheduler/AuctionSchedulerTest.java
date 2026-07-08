package scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import auction.ActivateScheduledAuctionsUseCase;
import auction.ProcessExpiredAuctionsUseCase;
import auction.output.ProcessAuctionsResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shared.NoInput;

@ExtendWith(MockitoExtension.class)
class AuctionSchedulerTest {

  @Mock private ProcessExpiredAuctionsUseCase processExpiredAuctionsUseCase;
  @Mock private ActivateScheduledAuctionsUseCase activateScheduledAuctionsUseCase;

  @InjectMocks private AuctionScheduler scheduler;

  @Test
  void closeExpiredAuctions_noneProcessed_invokesUseCase() {
    when(processExpiredAuctionsUseCase.run(ArgumentMatchers.any(NoInput.class)))
        .thenReturn(new ProcessAuctionsResult(0, 0));

    scheduler.closeExpiredAuctions();

    verify(processExpiredAuctionsUseCase).run(NoInput.INSTANCE);
  }

  @Test
  void closeExpiredAuctions_someProcessedAndFailed_invokesUseCaseWithoutThrowing() {
    when(processExpiredAuctionsUseCase.run(ArgumentMatchers.any(NoInput.class)))
        .thenReturn(new ProcessAuctionsResult(3, 1));

    scheduler.closeExpiredAuctions();

    verify(processExpiredAuctionsUseCase).run(NoInput.INSTANCE);
  }

  @Test
  void activateScheduledAuctions_noneProcessed_invokesUseCase() {
    when(activateScheduledAuctionsUseCase.run(ArgumentMatchers.any(NoInput.class)))
        .thenReturn(new ProcessAuctionsResult(0, 0));

    scheduler.activateScheduledAuctions();

    verify(activateScheduledAuctionsUseCase).run(NoInput.INSTANCE);
  }

  @Test
  void activateScheduledAuctions_someProcessedAndFailed_invokesUseCaseWithoutThrowing() {
    when(activateScheduledAuctionsUseCase.run(ArgumentMatchers.any(NoInput.class)))
        .thenReturn(new ProcessAuctionsResult(2, 1));

    scheduler.activateScheduledAuctions();

    verify(activateScheduledAuctionsUseCase).run(NoInput.INSTANCE);
  }
}
