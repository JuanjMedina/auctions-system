package adapter.out.persistence.user;

import domain.user.User;
import domain.user.UserRepository;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import repository.user.SpringDataUserRepository;

@Component
@AllArgsConstructor
public class UserJpaAdapter implements UserRepository {

  private final SpringDataUserRepository springDataRepo;

  @Override
  public User save(User user) {
    return null;
  }

  @Override
  public Optional<User> findById(String id) {
    return Optional.empty();
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return Optional.empty();
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return Optional.empty();
  }

  @Override
  public Optional<User> countActiveUsers() {
    return Optional.empty();
  }
}
