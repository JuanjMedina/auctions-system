package bid;

import bid.input.ListBidsInput;
import bid.output.ListBidsResult;
import bid.output.ListBidsResult.BidSummary;
import domain.auction.AuctionRepository;
import domain.bid.Bid;
import domain.bid.BidRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class ListBidsUseCase implements UseCase<ListBidsInput, ListBidsResult> {

  private final AuctionRepository auctionRepository;
  private final BidRepository bidRepository;

  @Override
  public ListBidsResult execute(ListBidsInput input) {
    auctionRepository.getById(input.auctionId());

    List<Bid> bids = bidRepository.findByAuctionIdOrderByAmountDesc(input.auctionId());

    List<BidSummary> summaries =
        bids.stream()
            .map(
                bid ->
                    new BidSummary(
                        bid.getId(),
                        bid.getBidderId(),
                        bid.getAmount(),
                        bid.isAutoBid(),
                        bid.getStatus(),
                        bid.getCreatedAt()))
            .toList();

    return new ListBidsResult(summaries);
  }

  @Override
  public String errorMessage() {
    return "Error al listar las pujas";
  }
}
