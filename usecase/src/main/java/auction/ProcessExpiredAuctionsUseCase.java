package auction;

import auction.input.CloseAuctionInput;
import auction.output.CloseAuctionResult;
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
public class ProcessExpiredAuctionsUseCase implements UseCase<NoInput, ProcessAuctionsResult> {

  private final AuctionRepository auctionRepository;
  private final CloseAuctionUseCase closeAuctionUseCase;

  @Override
  public ProcessAuctionsResult execute(NoInput input) {
    List<Auction> expired = auctionRepository.findExpiredActiveAuctions();

    if (expired.isEmpty()) return new ProcessAuctionsResult(0, 0);

    log.info("ProcessExpiredAuctions: {} subasta(s) expirada(s) detectada(s)", expired.size());

    int processed = 0;
    int failed = 0;

    for (Auction auction : expired) {
      try {
        CloseAuctionResult result = closeAuctionUseCase.run(new CloseAuctionInput(auction.getId()));
        log.info(
            "Subasta cerrada: id={} status={} winner={}",
            result.auctionId(),
            result.status(),
            result.winnerId());
        processed++;
      } catch (Exception ex) {
        log.error("Error al cerrar subasta id={}: {}", auction.getId(), ex.getMessage(), ex);
        failed++;
      }
    }

    return new ProcessAuctionsResult(processed, failed);
  }

  @Override
  public ProcessAuctionsResult failed(Exception exception) {
    log.error("Error en ProcessExpiredAuctionsUseCase: {}", exception.getMessage(), exception);
    return UseCase.super.failed(exception);
  }

  @Override
  public String errorMessage() {
    return "Error al procesar subastas expiradas";
  }
}
