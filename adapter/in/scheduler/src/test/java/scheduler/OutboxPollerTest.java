package scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import outbox.ProcessOutboxUseCase;
import outbox.input.ProcessOutboxInput;
import outbox.output.ProcessOutboxResult;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

  @Mock private ProcessOutboxUseCase processOutboxUseCase;

  @InjectMocks private OutboxPoller poller;

  @Test
  void poll_noneProcessed_invokesUseCaseWithDefaultInput() {
    when(processOutboxUseCase.run(ArgumentMatchers.any(ProcessOutboxInput.class)))
        .thenReturn(new ProcessOutboxResult(0, 0));

    poller.poll();

    verify(processOutboxUseCase).run(ProcessOutboxInput.DEFAULT);
  }

  @Test
  void poll_someProcessedAndFailed_invokesUseCaseWithoutThrowing() {
    when(processOutboxUseCase.run(ArgumentMatchers.any(ProcessOutboxInput.class)))
        .thenReturn(new ProcessOutboxResult(5, 2));

    poller.poll();

    verify(processOutboxUseCase).run(ProcessOutboxInput.DEFAULT);
  }
}
