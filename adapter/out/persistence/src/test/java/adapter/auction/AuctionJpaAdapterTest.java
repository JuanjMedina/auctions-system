package adapter.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.auction.Auction;
import domain.auction.AuctionImage;
import domain.auction.AuctionStatus;
import domain.shared.ConcurrencyException;
import domain.shared.PageResult;
import entity.auction.AuctionImageJpaEntity;
import entity.auction.AuctionJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import repository.auction.SpringDataAuctionRepository;

@ExtendWith(MockitoExtension.class)
class AuctionJpaAdapterTest {

  @Mock private SpringDataAuctionRepository springDataRepo;

  private AuctionJpaAdapter adapter;

  private Auction buildAuction(UUID id, AuctionStatus status, List<AuctionImage> images) {
    return Auction.reconstitute(
        id,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Subasta test",
        "Descripcion",
        BigDecimal.TEN,
        BigDecimal.valueOf(20),
        BigDecimal.valueOf(15),
        UUID.randomUUID(),
        status,
        Instant.now(),
        Instant.now().plusSeconds(3600),
        null,
        true,
        5,
        images,
        Instant.now(),
        Instant.now(),
        1L);
  }

  private AuctionJpaEntity buildEntity(UUID id, AuctionStatus status) {
    AuctionJpaEntity entity =
        AuctionJpaEntity.builder()
            .id(id)
            .sellerId(UUID.randomUUID())
            .categoryId(UUID.randomUUID())
            .title("Subasta entity")
            .description("Descripcion entity")
            .startingPrice(BigDecimal.TEN)
            .reservePrice(BigDecimal.valueOf(20))
            .currentPrice(BigDecimal.valueOf(15))
            .currentWinnerId(UUID.randomUUID())
            .status(status)
            .autoExtend(true)
            .extendMinutes(5)
            .startsAt(Instant.now())
            .endsAt(Instant.now().plusSeconds(3600))
            .closedAt(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .version(1L)
            .build();
    entity.setImages(List.of());
    return entity;
  }

  // --- setUp manual (constructor no trivial de mockear con InjectMocks por el spec lambda) ---

  private void init() {
    adapter = new AuctionJpaAdapter(springDataRepo);
  }

  // --- save ---

  @Test
  void save_validAuction_delegatesToSaveAndFlushAndMapsResult() {
    // arrange
    init();
    UUID imageId = UUID.randomUUID();
    Auction auction =
        buildAuction(
            UUID.randomUUID(),
            AuctionStatus.ACTIVE,
            List.of(new AuctionImage(imageId, "http://img", true, 0)));

    AuctionJpaEntity savedEntity = buildEntity(auction.getId(), AuctionStatus.ACTIVE);
    when(springDataRepo.saveAndFlush(any(AuctionJpaEntity.class))).thenReturn(savedEntity);

    // act
    Auction result = adapter.save(auction);

    // assert
    ArgumentCaptor<AuctionJpaEntity> captor = ArgumentCaptor.forClass(AuctionJpaEntity.class);
    verify(springDataRepo).saveAndFlush(captor.capture());
    AuctionJpaEntity captured = captor.getValue();

    assertThat(captured.getId()).isEqualTo(auction.getId());
    assertThat(captured.getSellerId()).isEqualTo(auction.getSellerId());
    assertThat(captured.getCategoryId()).isEqualTo(auction.getCategoryId());
    assertThat(captured.getTitle()).isEqualTo(auction.getTitle());
    assertThat(captured.getStartingPrice()).isEqualByComparingTo(auction.getStartingPrice());
    assertThat(captured.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
    assertThat(captured.getImages()).hasSize(1);
    assertThat(captured.getImages().get(0).getUrl()).isEqualTo("http://img");
    assertThat(captured.getImages().get(0).getAuction()).isSameAs(captured);

    assertThat(result.getId()).isEqualTo(savedEntity.getId());
    assertThat(result.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
  }

  @Test
  void save_optimisticLockingFailure_throwsConcurrencyException() {
    // arrange
    init();
    Auction auction = buildAuction(UUID.randomUUID(), AuctionStatus.ACTIVE, List.of());
    when(springDataRepo.saveAndFlush(any(AuctionJpaEntity.class)))
        .thenThrow(new OptimisticLockingFailureException("conflict"));

    // act & assert
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.save(auction))
        .isInstanceOf(ConcurrencyException.class);
  }

  // --- findById ---

  @Test
  void findById_existingAuction_returnsMappedDomain() {
    // arrange
    init();
    UUID id = UUID.randomUUID();
    AuctionJpaEntity entity = buildEntity(id, AuctionStatus.CLOSED);
    when(springDataRepo.findById(id)).thenReturn(Optional.of(entity));

    // act
    Optional<Auction> result = adapter.findById(id);

    // assert
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(id);
    assertThat(result.get().getStatus()).isEqualTo(AuctionStatus.CLOSED);
    assertThat(result.get().getSellerId()).isEqualTo(entity.getSellerId());
  }

  @Test
  void findById_missingAuction_returnsEmptyOptional() {
    // arrange
    init();
    UUID id = UUID.randomUUID();
    when(springDataRepo.findById(id)).thenReturn(Optional.empty());

    // act
    Optional<Auction> result = adapter.findById(id);

    // assert
    assertThat(result).isEmpty();
  }

  // --- findAll ---

  @Test
  void findAll_noFilters_delegatesAndMapsPageResult() {
    // arrange
    init();
    AuctionJpaEntity entity1 = buildEntity(UUID.randomUUID(), AuctionStatus.ACTIVE);
    AuctionJpaEntity entity2 = buildEntity(UUID.randomUUID(), AuctionStatus.CLOSED);
    Page<AuctionJpaEntity> page =
        new PageImpl<>(List.of(entity1, entity2), PageRequest.of(0, 10), 2);

    when(springDataRepo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

    // act
    PageResult<Auction> result = adapter.findAll(Optional.empty(), Optional.empty(), 0, 10);

    // assert
    assertThat(result.content()).hasSize(2);
    assertThat(result.totalElements()).isEqualTo(2L);
    assertThat(result.currentPage()).isZero();
    assertThat(result.pageSize()).isEqualTo(10);
  }

  @Test
  void findAll_emptyResult_returnsEmptyPageResult() {
    // arrange
    init();
    Page<AuctionJpaEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(springDataRepo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

    // act
    PageResult<Auction> result =
        adapter.findAll(Optional.of(AuctionStatus.ACTIVE), Optional.of(UUID.randomUUID()), 0, 10);

    // assert
    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isZero();
  }

  // --- findExpiredActiveAuctions ---

  @Test
  void findExpiredActiveAuctions_delegatesAndMapsResults() {
    // arrange
    init();
    AuctionJpaEntity entity = buildEntity(UUID.randomUUID(), AuctionStatus.EXTENDED);
    when(springDataRepo.findExpiredActiveAuctions()).thenReturn(List.of(entity));

    // act
    List<Auction> result = adapter.findExpiredActiveAuctions();

    // assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(AuctionStatus.EXTENDED);
  }

  @Test
  void findExpiredActiveAuctions_noResults_returnsEmptyList() {
    // arrange
    init();
    when(springDataRepo.findExpiredActiveAuctions()).thenReturn(List.of());

    // act
    List<Auction> result = adapter.findExpiredActiveAuctions();

    // assert
    assertThat(result).isEmpty();
  }

  // --- findScheduledReadyToStart ---

  @Test
  void findScheduledReadyToStart_delegatesAndMapsResults() {
    // arrange
    init();
    AuctionJpaEntity entity = buildEntity(UUID.randomUUID(), AuctionStatus.SCHEDULED);
    when(springDataRepo.findScheduledReadyToStart()).thenReturn(List.of(entity));

    // act
    List<Auction> result = adapter.findScheduledReadyToStart();

    // assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(AuctionStatus.SCHEDULED);
  }

  // --- findBySellerId ---

  @Test
  void findBySellerId_delegatesWithCorrectArgumentAndMapsResults() {
    // arrange
    init();
    UUID sellerId = UUID.randomUUID();
    AuctionJpaEntity entity = buildEntity(UUID.randomUUID(), AuctionStatus.ACTIVE);
    when(springDataRepo.findBySellerIdOrderByCreatedAtDesc(sellerId)).thenReturn(List.of(entity));

    // act
    List<Auction> result = adapter.findBySellerId(sellerId);

    // assert
    verify(springDataRepo).findBySellerIdOrderByCreatedAtDesc(eq(sellerId));
    assertThat(result).hasSize(1);
  }

  @Test
  void findBySellerId_noAuctions_returnsEmptyList() {
    // arrange
    init();
    UUID sellerId = UUID.randomUUID();
    when(springDataRepo.findBySellerIdOrderByCreatedAtDesc(sellerId)).thenReturn(List.of());

    // act
    List<Auction> result = adapter.findBySellerId(sellerId);

    // assert
    assertThat(result).isEmpty();
  }

  // --- findActiveByCategoryId ---

  @Test
  void findActiveByCategoryId_delegatesWithCorrectArgumentAndMapsResults() {
    // arrange
    init();
    UUID categoryId = UUID.randomUUID();
    AuctionJpaEntity entity = buildEntity(UUID.randomUUID(), AuctionStatus.ACTIVE);
    when(springDataRepo.findActiveByCategoryId(categoryId)).thenReturn(List.of(entity));

    // act
    List<Auction> result = adapter.findActiveByCategoryId(categoryId);

    // assert
    verify(springDataRepo).findActiveByCategoryId(eq(categoryId));
    assertThat(result).hasSize(1);
  }

  // --- existsByIdAndSellerId ---

  @Test
  void existsByIdAndSellerId_matchExists_returnsTrue() {
    // arrange
    init();
    UUID id = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    when(springDataRepo.existsByIdAndSellerId(id, sellerId)).thenReturn(true);

    // act
    boolean result = adapter.existsByIdAndSellerId(id, sellerId);

    // assert
    assertThat(result).isTrue();
  }

  @Test
  void existsByIdAndSellerId_noMatch_returnsFalse() {
    // arrange
    init();
    UUID id = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    when(springDataRepo.existsByIdAndSellerId(id, sellerId)).thenReturn(false);

    // act
    boolean result = adapter.existsByIdAndSellerId(id, sellerId);

    // assert
    assertThat(result).isFalse();
  }

  // --- mapeo de imagenes entity -> domain ---

  @Test
  void findById_entityWithImages_mapsImagesToDomain() {
    // arrange
    init();
    UUID id = UUID.randomUUID();
    AuctionJpaEntity entity = buildEntity(id, AuctionStatus.ACTIVE);
    UUID imageId = UUID.randomUUID();
    AuctionImageJpaEntity imageEntity =
        AuctionImageJpaEntity.builder()
            .id(imageId)
            .auction(entity)
            .url("http://image.test")
            .isPrimary(true)
            .displayOrder(1)
            .build();
    entity.setImages(List.of(imageEntity));

    when(springDataRepo.findById(id)).thenReturn(Optional.of(entity));

    // act
    Optional<Auction> result = adapter.findById(id);

    // assert
    assertThat(result).isPresent();
    List<AuctionImage> images = result.get().getImages();
    assertThat(images).hasSize(1);
    assertThat(images.get(0).getId()).isEqualTo(imageId);
    assertThat(images.get(0).getUrl()).isEqualTo("http://image.test");
    assertThat(images.get(0).isPrimary()).isTrue();
    assertThat(images.get(0).getDisplayOrder()).isEqualTo(1);
  }
}
