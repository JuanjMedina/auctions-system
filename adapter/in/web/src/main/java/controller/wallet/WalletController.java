package controller.wallet;

import controller.wallet.dto.DepositRequest;
import controller.wallet.dto.WithdrawRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import security.SecurityUtils;
import wallet.DepositUseCase;
import wallet.GetWalletTransactionsUseCase;
import wallet.GetWalletUseCase;
import wallet.WithdrawUseCase;
import wallet.input.DepositInput;
import wallet.input.GetWalletInput;
import wallet.input.GetWalletTransactionsInput;
import wallet.input.WithdrawInput;
import wallet.output.DepositResult;
import wallet.output.GetWalletResult;
import wallet.output.GetWalletTransactionsResult;
import wallet.output.WithdrawResult;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

  private final GetWalletUseCase getWalletUseCase;
  private final DepositUseCase depositUseCase;
  private final WithdrawUseCase withdrawUseCase;
  private final GetWalletTransactionsUseCase getWalletTransactionsUseCase;

  @GetMapping("/me")
  public ResponseEntity<GetWalletResult> getWallet() {
    UUID userId = SecurityUtils.currentUserId();
    return ResponseEntity.ok(getWalletUseCase.run(new GetWalletInput(userId)));
  }

  @PostMapping("/me/deposit")
  public ResponseEntity<DepositResult> deposit(@Valid @RequestBody DepositRequest request) {
    UUID userId = SecurityUtils.currentUserId();
    DepositInput input = new DepositInput(userId, request.amount(), request.description());
    return ResponseEntity.ok(depositUseCase.run(input));
  }

  @PostMapping("/me/withdraw")
  public ResponseEntity<WithdrawResult> withdraw(@Valid @RequestBody WithdrawRequest request) {
    UUID userId = SecurityUtils.currentUserId();
    WithdrawInput input = new WithdrawInput(userId, request.amount(), request.description());
    return ResponseEntity.ok(withdrawUseCase.run(input));
  }

  @GetMapping("/me/transactions")
  public ResponseEntity<GetWalletTransactionsResult> transactions() {
    UUID userId = SecurityUtils.currentUserId();
    return ResponseEntity.ok(
        getWalletTransactionsUseCase.run(new GetWalletTransactionsInput(userId)));
  }
}
