package controller;

import controller.dto.request.DepositRequest;
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
import wallet.GetWalletUseCase;
import wallet.input.DepositInput;
import wallet.input.GetWalletInput;
import wallet.output.DepositResult;
import wallet.output.GetWalletResult;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

  private final GetWalletUseCase getWalletUseCase;
  private final DepositUseCase depositUseCase;

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
}
