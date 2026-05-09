package adapter.out.persistence;

import domain.bid.Bid;
import domain.bid.BidRepository;
import entity.auction.BidJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import repository.SpringDataBidRepository;

@Component
@RequiredArgsConstructor
public class BidJpaAdapter implements BidRepository {

  private final SpringDataBidRepository springDataRepo;

  @Override
  public Bid save(Bid bid) {
    return toDomain(springDataRepo.save(toJpaEntity(bid)));
  }

  @Override
  public Optional<Bid> findById(UUID id) {
    return springDataRepo.findById(id).map(this::toDomain);
  }

  @Override
  public List<Bid> findByAuctionIdOrderByAmountDesc(UUID auctionId) {
    return springDataRepo.findByAuctionIdOrderByAmountDesc(auctionId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Bid> findByBidderId(UUID bidderId) {
    return springDataRepo.findByBidderIdOrderByCreatedAtDesc(bidderId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Bid> findActiveByAuctionId(UUID auctionId) {
    return springDataRepo.findActiveByAuctionId(auctionId).stream().map(this::toDomain).toList();
  }

  @Override
  public void saveAll(List<Bid> bids) {
    springDataRepo.saveAll(bids.stream().map(this::toJpaEntity).toList());
  }

  // Domain -> JpaEntity
  private BidJpaEntity toJpaEntity(Bid bid) {
    return BidJpaEntity.builder()
        .id(bid.getId())
        .auctionId(bid.getAuctionId())
        .bidderId(bid.getBidderId())
        .amount(bid.getAmount())
        .isAutoBid(bid.isAutoBid())
        .maxAutoAmount(bid.getMaxAutoAmount())
        .status(bid.getStatus())
        .build();
  }

  // JpaEntity -> Domain
  private Bid toDomain(BidJpaEntity entity) {
    return Bid.reconstitute(
        entity.getId(),
        entity.getAuctionId(),
        entity.getBidderId(),
        entity.getAmount(),
        entity.isAutoBid(),
        entity.getMaxAutoAmount(),
        entity.getStatus(),
        entity.getCreatedAt());
  }
}
