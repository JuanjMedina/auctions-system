package auction;

import auction.input.GetAuctionInput;
import auction.output.GetAuctionResult;
import auction.output.GetAuctionResult.AuctionImageResult;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;

@Service
@RequiredArgsConstructor
public class GetAuctionUseCase implements UseCase<GetAuctionInput, GetAuctionResult> {

  private final AuctionRepository auctionRepository;

  @Override
  public GetAuctionResult execute(GetAuctionInput input) {
    Auction auction = auctionRepository.getById(input.auctionId());
    return toResult(auction);
  }

  @Override
  public String errorMessage() {
    return "Error al obtener la subasta";
  }

  private GetAuctionResult toResult(Auction auction) {
    List<AuctionImageResult> images =
        auction.getImages().stream()
            .map(
                img ->
                    new AuctionImageResult(
                        img.getId(), img.getUrl(), img.isPrimary(), img.getDisplayOrder()))
            .toList();

    return new GetAuctionResult(
        auction.getId(),
        auction.getSellerId(),
        auction.getCategoryId(),
        auction.getTitle(),
        auction.getDescription(),
        auction.getStartingPrice(),
        auction.getReservePrice(),
        auction.getCurrentPrice(),
        auction.getCurrentWinnerId(),
        auction.getStatus(),
        auction.getStartsAt(),
        auction.getEndsAt(),
        auction.getClosedAt(),
        auction.isAutoExtend(),
        auction.getExtendMinutes(),
        images,
        auction.getCreatedAt(),
        auction.getUpdatedAt());
  }
}
