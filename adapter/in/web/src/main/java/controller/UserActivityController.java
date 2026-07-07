package controller;

import auction.ListMyAuctionsUseCase;
import auction.input.ListMyAuctionsInput;
import auction.output.ListMyAuctionsResult;
import bid.ListMyBidsUseCase;
import bid.input.ListMyBidsInput;
import bid.output.ListMyBidsResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import security.SecurityUtils;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserActivityController {

  private final ListMyBidsUseCase listMyBidsUseCase;
  private final ListMyAuctionsUseCase listMyAuctionsUseCase;

  @GetMapping("/bids")
  public ResponseEntity<ListMyBidsResult> myBids() {
    UUID userId = SecurityUtils.currentUserId();
    return ResponseEntity.ok(listMyBidsUseCase.run(new ListMyBidsInput(userId)));
  }

  @GetMapping("/auctions")
  public ResponseEntity<ListMyAuctionsResult> myAuctions() {
    UUID userId = SecurityUtils.currentUserId();
    return ResponseEntity.ok(listMyAuctionsUseCase.run(new ListMyAuctionsInput(userId)));
  }
}
