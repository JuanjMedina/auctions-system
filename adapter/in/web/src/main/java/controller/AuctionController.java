package controller;

import auction.CancelAuctionUseCase;
import auction.CloseAuctionUseCase;
import auction.CreateAuctionUseCase;
import auction.GetAuctionUseCase;
import auction.PublishAuctionUseCase;
import auction.input.CancelAuctionInput;
import auction.input.CloseAuctionInput;
import auction.input.CreateAuctionInput;
import auction.input.GetAuctionInput;
import auction.input.PublishAuctionInput;
import auction.output.CancelAuctionResult;
import auction.output.CloseAuctionResult;
import auction.output.CreateAuctionResult;
import auction.output.GetAuctionResult;
import auction.output.PublishAuctionResult;
import controller.dto.request.CreateAuctionRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import security.SecurityUtils;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionController {

  private final CreateAuctionUseCase createAuctionUseCase;
  private final GetAuctionUseCase getAuctionUseCase;
  private final PublishAuctionUseCase publishAuctionUseCase;
  private final CancelAuctionUseCase cancelAuctionUseCase;
  private final CloseAuctionUseCase closeAuctionUseCase;

  @PostMapping
  @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
  public ResponseEntity<CreateAuctionResult> create(
      @Valid @RequestBody CreateAuctionRequest request) {
    UUID sellerId = SecurityUtils.currentUserId();
    CreateAuctionInput input =
        new CreateAuctionInput(
            sellerId,
            request.categoryId(),
            request.title(),
            request.description(),
            request.startingPrice(),
            request.reservePrice(),
            request.startsAt(),
            request.endsAt(),
            request.autoExtend(),
            request.extendMinutes());
    return ResponseEntity.status(HttpStatus.CREATED).body(createAuctionUseCase.run(input));
  }

  @GetMapping("/{id}")
  public ResponseEntity<GetAuctionResult> get(@PathVariable UUID id) {
    return ResponseEntity.ok(getAuctionUseCase.run(new GetAuctionInput(id)));
  }

  @PatchMapping("/{id}/publish")
  @PreAuthorize("hasRole('SELLER')")
  public ResponseEntity<PublishAuctionResult> publish(@PathVariable UUID id) {
    UUID sellerId = SecurityUtils.currentUserId();
    return ResponseEntity.ok(publishAuctionUseCase.run(new PublishAuctionInput(id, sellerId)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('SELLER')")
  public ResponseEntity<CancelAuctionResult> cancel(@PathVariable UUID id) {
    UUID sellerId = SecurityUtils.currentUserId();
    return ResponseEntity.ok(cancelAuctionUseCase.run(new CancelAuctionInput(id, sellerId)));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CloseAuctionResult> close(@PathVariable UUID id) {
    return ResponseEntity.ok(closeAuctionUseCase.run(new CloseAuctionInput(id)));
  }
}
