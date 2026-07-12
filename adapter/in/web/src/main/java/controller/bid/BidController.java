package controller.bid;

import bid.DeleteBidUseCase;
import bid.ListBidsUseCase;
import bid.PlaceBidUseCase;
import bid.input.DeleteBidInput;
import bid.input.ListBidsInput;
import bid.input.PlaceBidInput;
import bid.output.DeleteBidOutput;
import bid.output.ListBidsResult;
import bid.output.PlaceBidOutput;
import controller.bid.dto.PlaceBidRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import security.SecurityUtils;

@RestController
@RequestMapping("/auctions/{auctionId}/bids")
@RequiredArgsConstructor
public class BidController {

  private final PlaceBidUseCase placeBidUseCase;
  private final ListBidsUseCase listBidsUseCase;
  private final DeleteBidUseCase deleteBidUseCase;

  @PostMapping
  @PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
  public ResponseEntity<PlaceBidOutput> placeBid(
      @PathVariable UUID auctionId, @Valid @RequestBody PlaceBidRequest request) {
    UUID bidderId = SecurityUtils.currentUserId();
    PlaceBidInput input =
        new PlaceBidInput(
            auctionId, bidderId, request.amount(), request.autoBid(), request.maxAutoAmount());
    return ResponseEntity.status(HttpStatus.CREATED).body(placeBidUseCase.run(input));
  }

  @GetMapping
  public ResponseEntity<ListBidsResult> listBids(@PathVariable UUID auctionId) {
    return ResponseEntity.ok(listBidsUseCase.run(new ListBidsInput(auctionId)));
  }

  @DeleteMapping("/{bidId}")
  @PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
  public ResponseEntity<DeleteBidOutput> deleteBid(
      @PathVariable UUID auctionId, @PathVariable UUID bidId) {
    UUID bidderId = SecurityUtils.currentUserId();
    DeleteBidInput input = new DeleteBidInput(bidId, auctionId, bidderId);
    return ResponseEntity.ok(deleteBidUseCase.run(input));
  }
}
