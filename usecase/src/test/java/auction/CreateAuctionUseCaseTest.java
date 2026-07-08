package auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import auction.input.CreateAuctionInput;
import auction.output.CreateAuctionResult;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateAuctionUseCaseTest {

  @Mock private AuctionRepository auctionRepository;

  @InjectMocks private CreateAuctionUseCase useCase;

  // --- fixtures ---

  private CreateAuctionInput validInput() {
    return new CreateAuctionInput(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Subasta test",
        "Descripcion",
        BigDecimal.TEN,
        BigDecimal.valueOf(20),
        Instant.now().plusSeconds(60),
        Instant.now().plusSeconds(3600),
        false,
        5);
  }

  // --- happy path ---

  @Test
  void execute_validInput_returnsCreatedAuctionResult() {
    // arrange
    when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

    // act
    CreateAuctionResult result = useCase.run(validInput());

    // assert
    assertThat(result.id()).isNotNull();
    assertThat(result.status()).isEqualTo(AuctionStatus.DRAFT);
    assertThat(result.createdAt()).isNotNull();
  }

  @Test
  void execute_validInput_savesAuctionInRepository() {
    // arrange
    when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

    // act
    useCase.run(validInput());

    // assert
    verify(auctionRepository).save(any(Auction.class));
  }

  // --- validacion de dominio propagada ---

  @Test
  void execute_invalidPrices_throwsIllegalArgumentException() {
    // arrange
    CreateAuctionInput input =
        new CreateAuctionInput(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Subasta test",
            "Descripcion",
            BigDecimal.TEN,
            BigDecimal.ONE, // reserva menor al precio inicial
            Instant.now().plusSeconds(60),
            Instant.now().plusSeconds(3600),
            false,
            5);

    // act & assert: run() delega en failed(), que relanza la RuntimeException original
    assertThatThrownBy(() -> useCase.run(input)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void execute_invalidPrices_neverCallsSave() {
    // arrange
    CreateAuctionInput input =
        new CreateAuctionInput(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Subasta test",
            "Descripcion",
            BigDecimal.TEN,
            BigDecimal.ONE, // reserva menor al precio inicial
            Instant.now().plusSeconds(60),
            Instant.now().plusSeconds(3600),
            false,
            5);

    // act
    assertThatThrownBy(() -> useCase.run(input)).isInstanceOf(IllegalArgumentException.class);

    // assert
    org.mockito.Mockito.verifyNoInteractions(auctionRepository);
  }
}
