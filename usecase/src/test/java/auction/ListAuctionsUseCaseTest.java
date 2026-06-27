package auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import auction.input.ListAuctionsInput;
import auction.output.ListAuctionsResult;
import domain.auction.Auction;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import domain.shared.PageResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListAuctionsUseCaseTest {

  @Mock private AuctionRepository auctionRepository;

  @InjectMocks private ListAuctionsUseCase useCase;

  private Auction buildAuction(AuctionStatus status) {
    return Auction.reconstitute(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Subasta test",
        "Descripcion",
        BigDecimal.TEN,
        BigDecimal.TEN,
        BigDecimal.TEN,
        null,
        status,
        Instant.now(),
        Instant.now().plusSeconds(3600),
        null,
        false,
        0,
        List.of(),
        Instant.now(),
        Instant.now(),
        0L);
  }

  // --- fixtures ---

  // --- sin filtros ---

  @Test
  void execute_noFilters_returnsFirstPageOfAuctions() {
    // arrange
    List<Auction> actions =
        List.of(buildAuction(AuctionStatus.ACTIVE), buildAuction(AuctionStatus.CLOSED));

    PageResult<Auction> pageResult = new PageResult<>(actions, 15L, 2, 0, 10);

    when(auctionRepository.findAll(Optional.empty(), Optional.empty(), 0, 10))
        .thenReturn(pageResult);

    // act

    ListAuctionsResult result =
        useCase.run(new ListAuctionsInput(Optional.empty(), Optional.empty(), 0, 10));

    // assert
    assertThat(result.page().content()).hasSize(2);
    assertThat(result.page().totalElements()).isEqualTo(15L);
    assertThat(result.page().currentPage()).isZero();
    assertThat(result.page().pageSize()).isEqualTo(10);
  }

  // --- filtro por status ---

  @Test
  void execute_filterByStatus_returnsOnlyMatchingAuctions() {
    // arrange

    List<Auction> actions =
        List.of(
            buildAuction(AuctionStatus.ACTIVE),
            buildAuction(AuctionStatus.ACTIVE),
            buildAuction(AuctionStatus.ACTIVE));

    PageResult<Auction> pageResult = new PageResult<>(actions, 3L, 1, 0, 10);

    when(auctionRepository.findAll(Optional.of(AuctionStatus.ACTIVE), Optional.empty(), 0, 10))
        .thenReturn(pageResult);
    // act

    ListAuctionsResult result =
        useCase.run(
            new ListAuctionsInput(Optional.of(AuctionStatus.ACTIVE), Optional.empty(), 0, 10));

    // assert
    assertThat(result.page().content())
        .hasSize(3)
        .allMatch(s -> s.status() == AuctionStatus.ACTIVE);

    assertThat(result.page().totalElements()).isEqualTo(3L);
  }

  // --- filtro por categoría ---

  @Test
  void execute_filterByCategoryId_returnsOnlyMatchingAuctions() {
    // arrange

    // act

    // assert
  }

  // --- combinación de filtros ---

  @Test
  void execute_filterByStatusAndCategory_returnsMatchingAuctions() {
    // arrange

    // act

    // assert
  }

  // --- paginación ---

  @Test
  void execute_secondPage_returnsCorrectContent() {
    // arrange

    // act

    // assert
  }

  @Test
  void execute_emptyResult_returnsEmptyPage() {
    // arrange

    // act

    // assert
  }
}
