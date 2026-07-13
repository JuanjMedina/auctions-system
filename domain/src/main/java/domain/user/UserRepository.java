package domain.user;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
  User save(User user);

  Optional<User> findById(UUID id);

  default User getById(UUID id) {
    return findById(id).orElseThrow(() -> new UserExceptions.UserNotFoundException(id));
  }

  Optional<User> findByEmail(String email);

  Optional<User> findByUsername(String username);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);
}
