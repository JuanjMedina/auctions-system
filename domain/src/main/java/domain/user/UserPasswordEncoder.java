package domain.user;

public interface UserPasswordEncoder {
  String encode(String rawPassword);

  boolean matches(String rawPassword, String hashedPassword);
}
