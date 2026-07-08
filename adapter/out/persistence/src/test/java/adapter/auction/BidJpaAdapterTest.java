package adapter.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.bid.Bid;
import domain.bid.BidStatus;
import domain.shared.ConcurrencyException;
import entity.auction.BidJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import repository.auction.SpringDataBidRepository;

@ExtendWith(MockitoExtension.class)
class BidJpaAdapterTest {

  @Mock private SpringDataBidRepository springDataRepo;

  private BidJpaAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new BidJpaAdapter(springDataRepo);
  }

  private Bid buildBid(UUID id, UUID auctionId, UUID bidderId, BidStatus status) {
    return Bid.reconstitute(
        id, auctionId, bidderId, BigDecimal.valueOf(100), false, null, status, Instant.now());
  }

  private BidJpaEntity buildEntity(UUID id, UUID auctionId, UUID bidderId, BidStatus status) {
    return BidJpaEntity.builder()
        .id(id)
        .auctionId(auctionId)
        .bidderId(bidderId)
        .amount(BigDecimal.valueOf(150))
        .isAutoBid(true)
        .maxAutoAmount(BigDecimal.valueOf(200))
        .status(status)
        .createdAt(Instant.now())
        .build();
  }

  // --- save ---

  @Test
  void save_validBid_delegatesToSaveAndMapsResult() {
    // arrange
    UUID id = UUID.randomUUID();
    UUID auctionId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    Bid bid = buildBid(id, auctionId, bidderId, BidStatus.ACTIVE);
    BidJpaEntity savedEntity = buildEntity(id, auctionId, bidderId, BidStatus.ACTIVE);

    when(springDataRepo.save(any(BidJpaEntity.class))).thenReturn(savedEntity);

    // act
    Bid result = adapter.save(bid);

    // assert
    ArgumentCaptor<BidJpaEntity> captor = ArgumentCaptor.forClass(BidJpaEntity.class);
    verify(springDataRepo).save(captor.capture());
    BidJpaEntity captured = captor.getValue();

    assertThat(captured.getId()).isEqualTo(id);
    assertThat(captured.getAuctionId()).isEqualTo(auctionId);
    assertThat(captured.getBidderId()).isEqualTo(bidderId);
    assertThat(captured.getAmount()).isEqualByComparingTo(bid.getAmount());
    assertThat(captured.isAutoBid()).isEqualTo(bid.isAutoBid());
    assertThat(captured.getStatus()).isEqualTo(BidStatus.ACTIVE);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getAmount()).isEqualByComparingTo(savedEntity.getAmount());
    assertThat(result.isAutoBid()).isTrue();
  }

  @Test
  void save_optimisticLockingFailure_throwsConcurrencyException() {
    // arrange
    Bid bid = buildBid(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BidStatus.ACTIVE);
    when(springDataRepo.save(any(BidJpaEntity.class)))
        .thenThrow(new OptimisticLockingFailureException("conflict"));

    // act & assert
    assertThatThrownBy(() -> adapter.save(bid)).isInstanceOf(ConcurrencyException.class);
  }

  // --- findById ---

  @Test
  void findById_existingBid_returnsMappedDomain() {
    // arrange
    UUID id = UUID.randomUUID();
    BidJpaEntity entity = buildEntity(id, UUID.randomUUID(), UUID.randomUUID(), BidStatus.WINNING);
    when(springDataRepo.findById(id)).thenReturn(Optional.of(entity));

    // act
    Optional<Bid> result = adapter.findById(id);

    // assert
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(id);
    assertThat(result.get().getStatus()).isEqualTo(BidStatus.WINNING);
  }

  @Test
  void findById_missingBid_returnsEmptyOptional() {
    // arrange
    UUID id = UUID.randomUUID();
    when(springDataRepo.findById(id)).thenReturn(Optional.empty());

    // act
    Optional<Bid> result = adapter.findById(id);

    // assert
    assertThat(result).isEmpty();
  }

  // --- findByAuctionIdOrderByAmountDesc ---

  @Test
  void findByAuctionIdOrderByAmountDesc_delegatesWithCorrectArgumentAndMapsResults() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    BidJpaEntity entity =
        buildEntity(UUID.randomUUID(), auctionId, UUID.randomUUID(), BidStatus.ACTIVE);
    when(springDataRepo.findByAuctionIdOrderByAmountDesc(auctionId)).thenReturn(List.of(entity));

    // act
    List<Bid> result = adapter.findByAuctionIdOrderByAmountDesc(auctionId);

    // assert
    verify(springDataRepo).findByAuctionIdOrderByAmountDesc(eq(auctionId));
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAuctionId()).isEqualTo(auctionId);
  }

  @Test
  void findByAuctionIdOrderByAmountDesc_noBids_returnsEmptyList() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    when(springDataRepo.findByAuctionIdOrderByAmountDesc(auctionId)).thenReturn(List.of());

    // act
    List<Bid> result = adapter.findByAuctionIdOrderByAmountDesc(auctionId);

    // assert
    assertThat(result).isEmpty();
  }

  // --- findByBidderId ---

  @Test
  void findByBidderId_delegatesWithCorrectArgumentAndMapsResults() {
    // arrange
    UUID bidderId = UUID.randomUUID();
    BidJpaEntity entity =
        buildEntity(UUID.randomUUID(), UUID.randomUUID(), bidderId, BidStatus.OUTBID);
    when(springDataRepo.findByBidderIdOrderByCreatedAtDesc(bidderId)).thenReturn(List.of(entity));

    // act
    List<Bid> result = adapter.findByBidderId(bidderId);

    // assert
    verify(springDataRepo).findByBidderIdOrderByCreatedAtDesc(eq(bidderId));
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getBidderId()).isEqualTo(bidderId);
  }

  // --- findActiveByAuctionId ---

  @Test
  void findActiveByAuctionId_delegatesWithCorrectArgumentAndMapsResults() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    BidJpaEntity entity =
        buildEntity(UUID.randomUUID(), auctionId, UUID.randomUUID(), BidStatus.ACTIVE);
    when(springDataRepo.findActiveByAuctionId(auctionId)).thenReturn(List.of(entity));

    // act
    List<Bid> result = adapter.findActiveByAuctionId(auctionId);

    // assert
    verify(springDataRepo).findActiveByAuctionId(eq(auctionId));
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(BidStatus.ACTIVE);
  }

  @Test
  void findActiveByAuctionId_noActiveBids_returnsEmptyList() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    when(springDataRepo.findActiveByAuctionId(auctionId)).thenReturn(List.of());

    // act
    List<Bid> result = adapter.findActiveByAuctionId(auctionId);

    // assert
    assertThat(result).isEmpty();
  }

  // --- saveAll ---

  @Test
  void saveAll_multipleBids_delegatesWithMappedEntities() {
    // arrange
    Bid bid1 = buildBid(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BidStatus.OUTBID);
    Bid bid2 = buildBid(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BidStatus.WINNING);

    // act
    adapter.saveAll(List.of(bid1, bid2));

    // assert
    ArgumentCaptor<List<BidJpaEntity>> captor = ArgumentCaptor.forClass(List.class);
    verify(springDataRepo).saveAll(captor.capture());
    List<BidJpaEntity> captured = captor.getValue();

    assertThat(captured).hasSize(2);
    assertThat(captured.get(0).getId()).isEqualTo(bid1.getId());
    assertThat(captured.get(0).getStatus()).isEqualTo(BidStatus.OUTBID);
    assertThat(captured.get(1).getId()).isEqualTo(bid2.getId());
    assertThat(captured.get(1).getStatus()).isEqualTo(BidStatus.WINNING);
  }

  @Test
  void saveAll_emptyList_delegatesWithEmptyList() {
    // act
    adapter.saveAll(List.of());

    // assert
    verify(springDataRepo).saveAll(anyList());
  }

  // --- findLatestActiveBidByAuctionIdAndBidderId ---

  @Test
  void findLatestActiveBidByAuctionIdAndBidderId_existingBid_returnsMappedDomain() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    BidJpaEntity entity = buildEntity(UUID.randomUUID(), auctionId, bidderId, BidStatus.ACTIVE);
    when(springDataRepo.findLatestActiveBidByAuctionIdAndBidderId(auctionId, bidderId))
        .thenReturn(Optional.of(entity));

    // act
    Optional<Bid> result = adapter.findLatestActiveBidByAuctionIdAndBidderId(auctionId, bidderId);

    // assert
    assertThat(result).isPresent();
    assertThat(result.get().getAuctionId()).isEqualTo(auctionId);
    assertThat(result.get().getBidderId()).isEqualTo(bidderId);
  }

  @Test
  void findLatestActiveBidByAuctionIdAndBidderId_noMatch_returnsEmptyOptional() {
    // arrange
    UUID auctionId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    when(springDataRepo.findLatestActiveBidByAuctionIdAndBidderId(auctionId, bidderId))
        .thenReturn(Optional.empty());

    // act
    Optional<Bid> result = adapter.findLatestActiveBidByAuctionIdAndBidderId(auctionId, bidderId);

    // assert
    assertThat(result).isEmpty();
  }
}
