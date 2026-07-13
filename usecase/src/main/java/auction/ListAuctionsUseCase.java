package auction;

import auction.input.ListAuctionsInput;
import auction.output.AuctionSummary;
import auction.output.ListAuctionsResult;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import domain.shared.PageResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class ListAuctionsUseCase implements UseCase<ListAuctionsInput, ListAuctionsResult> {

  private final AuctionRepository auctionRepository;
  private static final int DEFAULT_PAGE_SIZE = 10;
  private static final int DEFAULT_PAGE_NUMBER = 0;

  @Override
  public ListAuctionsResult execute(ListAuctionsInput input) {
    int page = input.page() == null ? DEFAULT_PAGE_NUMBER : input.page();
    int size = input.size() == null ? DEFAULT_PAGE_SIZE : input.size();

    PageResult<Auction> auctions =
        auctionRepository.findAll(input.status(), input.categoryId(), page, size);

    List<AuctionSummary> content =
        auctions.content().stream().map(ListAuctionsUseCase::toAuctionSummary).toList();

    PageResult<AuctionSummary> pageResult =
        PageResult.<AuctionSummary>builder()
            .content(content)
            .totalElements(auctions.totalElements())
            .totalPages(auctions.totalPages())
            .currentPage(page)
            .pageSize(size)
            .build();

    return new ListAuctionsResult(pageResult);
  }

  private static AuctionSummary toAuctionSummary(Auction auction) {
    return AuctionSummary.builder()
        .id(auction.getId())
        .title(auction.getTitle())
        .description(auction.getDescription())
        .categoryId(auction.getCategoryId())
        .sellerId(auction.getSellerId())
        .currentPrice(auction.getCurrentPrice())
        .status(auction.getStatus())
        .startsAt(auction.getStartsAt())
        .endsAt(auction.getEndsAt())
        .build();
  }

  @Override
  public String errorMessage() {
    return "Error al listar subastas";
  }
}
