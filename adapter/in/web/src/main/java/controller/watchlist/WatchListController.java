package controller.watchlist;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import security.SecurityUtils;
import watchList.AddToWatchListUseCase;
import watchList.ListWatchListUseCase;
import watchList.RemoveFromWatchListUseCase;
import watchList.input.AddToWatchListInput;
import watchList.input.ListWatchListInput;
import watchList.input.RemoveFromWatchListInput;
import watchList.output.AddToWatchListResult;
import watchList.output.ListWatchListResult;
import watchList.output.RemoveFromWatchListResult;

@RestController
@RequestMapping("/watchlist")
@RequiredArgsConstructor
public class WatchListController {

  private final AddToWatchListUseCase addToWatchListUseCase;
  private final RemoveFromWatchListUseCase removeFromWatchListUseCase;
  private final ListWatchListUseCase listWatchListUseCase;

  @PostMapping("/{auctionId}")
  public ResponseEntity<AddToWatchListResult> add(@PathVariable UUID auctionId) {
    UUID userId = SecurityUtils.currentUserId();
    AddToWatchListResult result =
        addToWatchListUseCase.run(new AddToWatchListInput(userId, auctionId));
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @DeleteMapping("/{auctionId}")
  public ResponseEntity<RemoveFromWatchListResult> remove(@PathVariable UUID auctionId) {
    UUID userId = SecurityUtils.currentUserId();
    RemoveFromWatchListResult result =
        removeFromWatchListUseCase.run(new RemoveFromWatchListInput(userId, auctionId));
    return ResponseEntity.ok(result);
  }

  @GetMapping
  public ResponseEntity<ListWatchListResult> list() {
    UUID userId = SecurityUtils.currentUserId();
    return ResponseEntity.ok(listWatchListUseCase.run(new ListWatchListInput(userId)));
  }
}
