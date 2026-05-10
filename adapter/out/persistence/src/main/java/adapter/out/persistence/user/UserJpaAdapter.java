package adapter.out.persistence.user;

import domain.user.User;
import domain.user.UserRepository;
import entity.user.UserEntity;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import repository.user.SpringDataUserRepository;

@Component
@RequiredArgsConstructor
public class UserJpaAdapter implements UserRepository {

  private final SpringDataUserRepository springDataRepo;

  @Override
  public User save(User user) {
    return toDomain(springDataRepo.save(toJpaEntity(user)));
  }

  @Override
  public Optional<User> findById(UUID id) {
    return springDataRepo.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return springDataRepo.findByEmail(email).map(this::toDomain);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return springDataRepo.findByUsername(username).map(this::toDomain);
  }

  @Override
  public boolean existsByEmail(String email) {
    return springDataRepo.existsByEmail(email);
  }

  @Override
  public boolean existsByUsername(String username) {
    return springDataRepo.existsByUsername(username);
  }

  private UserEntity toJpaEntity(User user) {
    return UserEntity.builder()
        .id(user.getId())
        .email(user.getEmail())
        .username(user.getUsername())
        .passwordHash(user.getPasswordHash())
        .fullName(user.getFullName())
        .phone(user.getPhone())
        .role(user.getRole())
        .isActive(user.isActive())
        .build();
  }

  private User toDomain(UserEntity entity) {
    return User.reconstitute(
        entity.getId(),
        entity.getEmail(),
        entity.getUsername(),
        entity.getPasswordHash(),
        entity.getFullName(),
        entity.getPhone(),
        entity.getRole(),
        entity.isActive(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
