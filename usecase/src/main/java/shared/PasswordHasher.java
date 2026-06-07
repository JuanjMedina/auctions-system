package shared;

public interface PasswordHasher {
  String hash(String rawPassword);
}
