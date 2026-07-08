package adapter.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.categories.Category;
import entity.category.CategoryJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.category.SpringDataCategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryJpaAdapterTest {

  @Mock private SpringDataCategoryRepository springDataRepo;

  private CategoryJpaAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new CategoryJpaAdapter(springDataRepo);
  }

  private Category buildCategory(UUID id, UUID parentId, boolean isActive) {
    return Category.reconstitute(id, "Electronics", "electronics", parentId, isActive);
  }

  private CategoryJpaEntity buildEntity(UUID id, UUID parentId, boolean isActive) {
    return CategoryJpaEntity.builder()
        .id(id)
        .name("Electronics")
        .slug("electronics")
        .parentId(parentId)
        .isActive(isActive)
        .build();
  }

  // --- save ---

  @Test
  void save_rootCategory_delegatesWithNullParentAndMapsResult() {
    // arrange
    UUID id = UUID.randomUUID();
    Category category = buildCategory(id, null, true);
    CategoryJpaEntity savedEntity = buildEntity(id, null, true);

    when(springDataRepo.save(any(CategoryJpaEntity.class))).thenReturn(savedEntity);

    // act
    Category result = adapter.save(category);

    // assert
    ArgumentCaptor<CategoryJpaEntity> captor = ArgumentCaptor.forClass(CategoryJpaEntity.class);
    verify(springDataRepo).save(captor.capture());
    CategoryJpaEntity captured = captor.getValue();

    assertThat(captured.getId()).isEqualTo(id);
    assertThat(captured.getName()).isEqualTo("Electronics");
    assertThat(captured.getSlug()).isEqualTo("electronics");
    assertThat(captured.getParent()).isNull();
    assertThat(captured.isActive()).isTrue();

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.isRoot()).isTrue();
  }

  @Test
  void save_childCategory_delegatesWithParentReferenceAndMapsResult() {
    // arrange
    UUID id = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    Category category = buildCategory(id, parentId, true);
    CategoryJpaEntity savedEntity = buildEntity(id, parentId, true);

    when(springDataRepo.save(any(CategoryJpaEntity.class))).thenReturn(savedEntity);

    // act
    Category result = adapter.save(category);

    // assert
    ArgumentCaptor<CategoryJpaEntity> captor = ArgumentCaptor.forClass(CategoryJpaEntity.class);
    verify(springDataRepo).save(captor.capture());
    CategoryJpaEntity captured = captor.getValue();

    assertThat(captured.getParent()).isNotNull();
    assertThat(captured.getParent().getId()).isEqualTo(parentId);

    assertThat(result.getParentId()).isEqualTo(parentId);
    assertThat(result.isRoot()).isFalse();
  }

  // --- findById ---

  @Test
  void findById_existingCategory_returnsMappedDomain() {
    // arrange
    UUID id = UUID.randomUUID();
    CategoryJpaEntity entity = buildEntity(id, null, true);
    when(springDataRepo.findById(id)).thenReturn(Optional.of(entity));

    // act
    Optional<Category> result = adapter.findById(id);

    // assert
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(id);
    assertThat(result.get().getName()).isEqualTo("Electronics");
  }

  @Test
  void findById_missingCategory_returnsEmptyOptional() {
    // arrange
    UUID id = UUID.randomUUID();
    when(springDataRepo.findById(id)).thenReturn(Optional.empty());

    // act
    Optional<Category> result = adapter.findById(id);

    // assert
    assertThat(result).isEmpty();
  }

  // --- findBySlug ---

  @Test
  void findBySlug_existingSlug_returnsMappedDomain() {
    // arrange
    CategoryJpaEntity entity = buildEntity(UUID.randomUUID(), null, true);
    when(springDataRepo.findBySlug("electronics")).thenReturn(Optional.of(entity));

    // act
    Optional<Category> result = adapter.findBySlug("electronics");

    // assert
    assertThat(result).isPresent();
    assertThat(result.get().getSlug()).isEqualTo("electronics");
  }

  @Test
  void findBySlug_missingSlug_returnsEmptyOptional() {
    // arrange
    when(springDataRepo.findBySlug("unknown")).thenReturn(Optional.empty());

    // act
    Optional<Category> result = adapter.findBySlug("unknown");

    // assert
    assertThat(result).isEmpty();
  }

  // --- findAllActive ---

  @Test
  void findAllActive_delegatesToFindByIsActiveTrueAndMapsResults() {
    // arrange
    CategoryJpaEntity entity = buildEntity(UUID.randomUUID(), null, true);
    when(springDataRepo.findByIsActiveTrue()).thenReturn(List.of(entity));

    // act
    List<Category> result = adapter.findAllActive();

    // assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).isActive()).isTrue();
  }

  @Test
  void findAllActive_noneActive_returnsEmptyList() {
    // arrange
    when(springDataRepo.findByIsActiveTrue()).thenReturn(List.of());

    // act
    List<Category> result = adapter.findAllActive();

    // assert
    assertThat(result).isEmpty();
  }

  // --- findRootCategories ---

  @Test
  void findRootCategories_delegatesToFindByParentIsNullAndIsActiveTrueAndMapsResults() {
    // arrange
    CategoryJpaEntity entity = buildEntity(UUID.randomUUID(), null, true);
    when(springDataRepo.findByParentIsNullAndIsActiveTrue()).thenReturn(List.of(entity));

    // act
    List<Category> result = adapter.findRootCategories();

    // assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).isRoot()).isTrue();
  }

  // --- findByParentId ---

  @Test
  void findByParentId_delegatesWithCorrectArgumentAndMapsResults() {
    // arrange
    UUID parentId = UUID.randomUUID();
    CategoryJpaEntity entity = buildEntity(UUID.randomUUID(), parentId, true);
    when(springDataRepo.findByParent_Id(parentId)).thenReturn(List.of(entity));

    // act
    List<Category> result = adapter.findByParentId(parentId);

    // assert
    verify(springDataRepo).findByParent_Id(eq(parentId));
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getParentId()).isEqualTo(parentId);
  }

  @Test
  void findByParentId_noChildren_returnsEmptyList() {
    // arrange
    UUID parentId = UUID.randomUUID();
    when(springDataRepo.findByParent_Id(parentId)).thenReturn(List.of());

    // act
    List<Category> result = adapter.findByParentId(parentId);

    // assert
    assertThat(result).isEmpty();
  }

  // --- existsBySlug ---

  @Test
  void existsBySlug_slugExists_returnsTrue() {
    // arrange
    when(springDataRepo.existsBySlug("electronics")).thenReturn(true);

    // act
    boolean result = adapter.existsBySlug("electronics");

    // assert
    assertThat(result).isTrue();
  }

  @Test
  void existsBySlug_slugMissing_returnsFalse() {
    // arrange
    when(springDataRepo.existsBySlug("unknown")).thenReturn(false);

    // act
    boolean result = adapter.existsBySlug("unknown");

    // assert
    assertThat(result).isFalse();
  }
}
