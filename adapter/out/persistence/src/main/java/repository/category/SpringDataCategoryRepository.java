package repository.category;

import entity.category.CategoryJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataCategoryRepository extends JpaRepository<CategoryJpaEntity, UUID> {
  Optional<CategoryJpaEntity> findBySlug(String slug);

  List<CategoryJpaEntity> findByIsActiveTrue();

  List<CategoryJpaEntity> findByParentIsNullAndIsActiveTrue();

  List<CategoryJpaEntity> findByParent_Id(UUID parentId);

  boolean existsBySlug(String slug);
}
