package adapter.auction;

import domain.auction.Auction;
import domain.auction.AuctionImage;
import domain.auction.AuctionRepository;
import domain.auction.AuctionStatus;
import domain.shared.ConcurrencyException;
import domain.shared.PageResult;
import entity.auction.AuctionImageJpaEntity;
import entity.auction.AuctionJpaEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import repository.auction.SpringDataAuctionRepository;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionJpaAdapter implements AuctionRepository {

  private final SpringDataAuctionRepository springDataRepo;

  @Override
  @Transactional
  public Auction save(Auction auction) {
    try {
      return toDomain(springDataRepo.saveAndFlush(toJpaEntity(auction)));
    } catch (OptimisticLockingFailureException e) {
      throw new ConcurrencyException("Auction", auction.getId());
    }
  }

  @Override
  public Optional<Auction> findById(UUID id) {
    return springDataRepo.findById(id).map(this::toDomain);
  }

  @Override
  public PageResult<Auction> findAll(
      Optional<AuctionStatus> status, Optional<UUID> categoryId, int page, int size) {

    Specification<AuctionJpaEntity> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          status.ifPresent(s -> predicates.add(cb.equal(root.get("status"), s)));
          categoryId.ifPresent(c -> predicates.add(cb.equal(root.get("categoryId"), c)));
          return cb.and(predicates.toArray(new Predicate[0]));
        };

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<AuctionJpaEntity> result = springDataRepo.findAll(spec, pageRequest);

    List<Auction> content = result.getContent().stream().map(this::toDomain).toList();

    return new PageResult<>(
        content,
        result.getTotalElements(),
        result.getTotalPages(),
        result.getNumber(),
        result.getSize());
  }

  @Override
  public List<Auction> findExpiredActiveAuctions() {
    return springDataRepo.findExpiredActiveAuctions().stream().map(this::toDomain).toList();
  }

  @Override
  public List<Auction> findScheduledReadyToStart() {
    return springDataRepo.findScheduledReadyToStart().stream().map(this::toDomain).toList();
  }

  @Override
  public List<Auction> findBySellerId(UUID sellerId) {
    return springDataRepo.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Auction> findActiveByCategoryId(UUID categoryId) {
    return springDataRepo.findActiveByCategoryId(categoryId).stream().map(this::toDomain).toList();
  }

  @Override
  public boolean existsByIdAndSellerId(UUID id, UUID sellerId) {
    return springDataRepo.existsByIdAndSellerId(id, sellerId);
  }

  // Domain -> JpaEntity
  private AuctionJpaEntity toJpaEntity(Auction auction) {
    AuctionJpaEntity entity =
        AuctionJpaEntity.builder()
            .id(auction.getId())
            .sellerId(auction.getSellerId())
            .categoryId(auction.getCategoryId())
            .title(auction.getTitle())
            .description(auction.getDescription())
            .startingPrice(auction.getStartingPrice())
            .reservePrice(auction.getReservePrice())
            .currentPrice(auction.getCurrentPrice())
            .currentWinnerId(auction.getCurrentWinnerId())
            .status(auction.getStatus())
            .autoExtend(auction.isAutoExtend())
            .extendMinutes(auction.getExtendMinutes())
            .startsAt(auction.getStartsAt())
            .endsAt(auction.getEndsAt())
            .closedAt(auction.getClosedAt())
            .version(auction.getVersion())
            .build();

    List<AuctionImageJpaEntity> imageEntities =
        auction.getImages().stream()
            .map(
                img ->
                    AuctionImageJpaEntity.builder()
                        .id(img.getId())
                        .auction(entity)
                        .url(img.getUrl())
                        .isPrimary(img.isPrimary())
                        .displayOrder(img.getDisplayOrder())
                        .build())
            .toList();
    entity.setImages(imageEntities);
    return entity;
  }

  // JpaEntity -> Domain
  private Auction toDomain(AuctionJpaEntity entity) {
    List<AuctionImage> images =
        entity.getImages().stream()
            .map(
                img ->
                    new AuctionImage(
                        img.getId(), img.getUrl(), img.isPrimary(), img.getDisplayOrder()))
            .toList();

    return Auction.reconstitute(
        entity.getId(),
        entity.getSellerId(),
        entity.getCategoryId(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getStartingPrice(),
        entity.getReservePrice(),
        entity.getCurrentPrice(),
        entity.getCurrentWinnerId(),
        entity.getStatus(),
        entity.getStartsAt(),
        entity.getEndsAt(),
        entity.getClosedAt(),
        entity.isAutoExtend(),
        entity.getExtendMinutes(),
        images,
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getVersion());
  }
}
