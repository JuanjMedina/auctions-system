package auction;

import auction.input.ActivateAuctionInput;
import auction.output.ActivateAuctionResult;
import auction.output.ProcessAuctionsResult;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import shared.NoInput;
import shared.UseCase;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivateScheduledAuctionsUseCase implements UseCase<NoInput, ProcessAuctionsResult> {

  private final AuctionRepository auctionRepository;
  private final ActivateAuctionUseCase activateAuctionUseCase;

  @Override
  public ProcessAuctionsResult execute(NoInput input) {
    List<Auction> readyToStart = auctionRepository.findScheduledReadyToStart();

    if (readyToStart.isEmpty()) return new ProcessAuctionsResult(0, 0);

    log.info("ActivateScheduledAuctions: {} subasta(s) lista(s) para activar", readyToStart.size());

    int processed = 0;
    int failed = 0;

    for (Auction auction : readyToStart) {
      try {
        ActivateAuctionResult result =
            activateAuctionUseCase.run(new ActivateAuctionInput(auction.getId()));
        log.info("Subasta activada: id={} status={}", result.id(), result.status());
        processed++;
      } catch (Exception ex) {
        log.error("Error al activar subasta id={}: {}", auction.getId(), ex.getMessage(), ex);
        failed++;
      }
    }

    return new ProcessAuctionsResult(processed, failed);
  }

  @Override
  public ProcessAuctionsResult failed(Exception exception) {
    log.error("Error en ActivateScheduledAuctionsUseCase: {}", exception.getMessage(), exception);
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al activar subastas programadas", exception);
  }
}
