package domain.user;

public interface PasswordHasher {
  String hash(String rawPassword);
}
