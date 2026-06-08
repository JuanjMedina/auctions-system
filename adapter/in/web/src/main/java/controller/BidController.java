package controller;

import bid.PlaceBidUseCase;
import bid.input.PlaceBidInput;
import bid.output.PlaceBidOutput;
import controller.dto.request.PlaceBidRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

  @PostMapping
  @PreAuthorize("hasRole('BUYER')")
  public ResponseEntity<PlaceBidOutput> placeBid(
      @PathVariable UUID auctionId, @Valid @RequestBody PlaceBidRequest request) {
    UUID bidderId = SecurityUtils.currentUserId();
    PlaceBidInput input =
        new PlaceBidInput(
            auctionId, bidderId, request.amount(), request.autoBid(), request.maxAutoAmount());
    return ResponseEntity.status(HttpStatus.CREATED).body(placeBidUseCase.run(input));
  }
}
