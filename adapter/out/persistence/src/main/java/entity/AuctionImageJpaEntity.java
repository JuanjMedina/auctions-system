package entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "auction_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuctionImageJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private AuctionJpaEntity auction;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
