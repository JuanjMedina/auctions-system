package adapter.category;

import domain.categories.Category;
import domain.categories.CategoryRepository;
import entity.category.CategoryJpaEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import repository.category.SpringDataCategoryRepository;

@Component
@RequiredArgsConstructor
public class CategoryJpaAdapter implements CategoryRepository {

  private final SpringDataCategoryRepository springDataRepo;
  private final EntityManager entityManager;

  @Override
  public Category save(Category category) {
    return toDomain(springDataRepo.save(toJpaEntity(category)));
  }

  @Override
  public Optional<Category> findById(UUID id) {
    return springDataRepo.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<Category> findBySlug(String slug) {
    return springDataRepo.findBySlug(slug).map(this::toDomain);
  }

  @Override
  public List<Category> findAllActive() {
    return springDataRepo.findByIsActiveTrue().stream().map(this::toDomain).toList();
  }

  @Override
  public List<Category> findRootCategories() {
    return springDataRepo.findByParentIsNullAndIsActiveTrue().stream().map(this::toDomain).toList();
  }

  @Override
  public List<Category> findByParentId(UUID parentId) {
    return springDataRepo.findByParent_Id(parentId).stream().map(this::toDomain).toList();
  }

  @Override
  public boolean existsBySlug(String slug) {
    return springDataRepo.existsBySlug(slug);
  }

  private CategoryJpaEntity toJpaEntity(Category category) {
    // entityManager.getReference (no un CategoryJpaEntity "a mano") produce un proxy que
    // Hibernate reconoce como existente sin cargarlo; ver WalletJpaAdapter.toJpaEntity.
    CategoryJpaEntity parent =
        category.getParentId() != null
            ? entityManager.getReference(CategoryJpaEntity.class, category.getParentId())
            : null;

    return CategoryJpaEntity.builder()
        .id(category.getId())
        .name(category.getName())
        .slug(category.getSlug())
        .parent(parent)
        .isActive(category.isActive())
        .build();
  }

  private Category toDomain(CategoryJpaEntity entity) {
    return Category.reconstitute(
        entity.getId(),
        entity.getName(),
        entity.getSlug(),
        entity.getParentId(),
        entity.isActive());
  }
}
