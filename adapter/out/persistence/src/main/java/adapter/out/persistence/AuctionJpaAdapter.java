package adapter.out.persistence;

import domain.auction.Auction;
import domain.auction.AuctionImage;
import domain.auction.AuctionRepository;
import entity.AuctionImageJpaEntity;
import entity.AuctionJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import repository.SpringDataAuctionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuctionJpaAdapter implements AuctionRepository {

    private final SpringDataAuctionRepository springDataRepo;

    @Override
    public Auction save(Auction auction) {
        return toDomain(springDataRepo.save(toJpaEntity(auction)));
    }

    @Override
    public Optional<Auction> findById(UUID id) {
        return springDataRepo.findById(id).map(this::toDomain);
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
        return springDataRepo.findBySellerIdOrderByCreatedAtDesc(sellerId).stream().map(this::toDomain).toList();
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
        AuctionJpaEntity entity = AuctionJpaEntity.builder()
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

        List<AuctionImageJpaEntity> imageEntities = auction.getImages().stream()
                .map(img -> AuctionImageJpaEntity.builder()
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
        List<AuctionImage> images = entity.getImages().stream()
                .map(img -> new AuctionImage(img.getId(), img.getUrl(), img.isPrimary(), img.getDisplayOrder()))
                .toList();

        return Auction.reconstitute(
                entity.getId(), entity.getSellerId(), entity.getCategoryId(),
                entity.getTitle(), entity.getDescription(),
                entity.getStartingPrice(), entity.getReservePrice(),
                entity.getCurrentPrice(), entity.getCurrentWinnerId(),
                entity.getStatus(),
                entity.getStartsAt(), entity.getEndsAt(), entity.getClosedAt(),
                entity.isAutoExtend(), entity.getExtendMinutes(),
                images,
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion()
        );
    }
}
