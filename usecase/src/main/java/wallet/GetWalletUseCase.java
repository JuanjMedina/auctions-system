package wallet;

import domain.wallets.Wallet;
import domain.wallets.WalletExceptions;
import domain.wallets.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shared.UseCase;
import wallet.input.GetWalletInput;
import wallet.output.GetWalletResult;

@Service
@RequiredArgsConstructor
public class GetWalletUseCase implements UseCase<GetWalletInput, GetWalletResult> {

  private final WalletRepository walletRepository;

  @Override
  public GetWalletResult execute(GetWalletInput input) {
    Wallet wallet =
        walletRepository
            .findByUserId(input.userId())
            .orElseThrow(() -> new WalletExceptions.WalletNotFoundException(input.userId()));

    return new GetWalletResult(
        wallet.getId(),
        wallet.getUserId(),
        wallet.getBalance(),
        wallet.getReservedBalance(),
        wallet.availableBalance(),
        wallet.getCurrency(),
        wallet.getUpdatedAt());
  }

  @Override
  public GetWalletResult failed(Exception exception) {
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException("Error al obtener la billetera", exception);
  }
}
