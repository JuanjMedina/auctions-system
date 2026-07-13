package bid;

import bid.input.ListMyBidsInput;
import bid.output.ListMyBidsResult;
import bid.output.ListMyBidsResult.MyBidSummary;
import domain.bid.Bid;
import domain.bid.BidRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class ListMyBidsUseCase implements UseCase<ListMyBidsInput, ListMyBidsResult> {

  private final BidRepository bidRepository;

  @Override
  public ListMyBidsResult execute(ListMyBidsInput input) {
    List<Bid> bids = bidRepository.findByBidderId(input.bidderId());

    List<MyBidSummary> summaries =
        bids.stream()
            .map(
                bid ->
                    new MyBidSummary(
                        bid.getId(),
                        bid.getAuctionId(),
                        bid.getAmount(),
                        bid.isAutoBid(),
                        bid.getStatus(),
                        bid.getCreatedAt()))
            .toList();

    return new ListMyBidsResult(summaries);
  }

  @Override
  public String errorMessage() {
    return "Error al listar mis pujas";
  }
}
