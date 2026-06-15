package entity.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryJpaEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, unique = true, length = 100)
  private String slug;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private CategoryJpaEntity parent;

  @Column(name = "parent_id", insertable = false, updatable = false)
  private UUID parentId;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
  @Builder.Default
  private List<CategoryJpaEntity> children = new ArrayList<>();
}
