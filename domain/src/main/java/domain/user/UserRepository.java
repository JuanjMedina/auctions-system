package domain.user;

import java.util.Optional;

public interface UserRepository {
  User save(User user);

  Optional<User> findById(String id);

  Optional<User> findByEmail(String email);

  Optional<User> findByUsername(String username);

  Optional<User> countActiveUsers();
}
