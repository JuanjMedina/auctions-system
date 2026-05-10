package domain.user;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
public class User {

  private final UUID id;
  private String email;
  private String username;
  private String passwordHash;
  private String fullName;
  private String phone;
  private Role role;
  private boolean isActive;
  private final Instant createdAt;
  private Instant updatedAt;

  private User(
      UUID id,
      String email,
      String username,
      String passwordHash,
      String fullName,
      String phone,
      Role role,
      boolean isActive,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.email = email;
    this.username = username;
    this.passwordHash = passwordHash;
    this.fullName = fullName;
    this.phone = phone;
    this.role = role;
    this.isActive = isActive;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static User create(
      String email,
      String username,
      String passwordHash,
      String fullName,
      String phone,
      Role role) {
    Instant now = Instant.now();
    return new User(
        UUID.randomUUID(), email, username, passwordHash, fullName, phone, role, true, now, now);
  }

  public static User reconstitute(
      UUID id,
      String email,
      String username,
      String passwordHash,
      String fullName,
      String phone,
      Role role,
      boolean isActive,
      Instant createdAt,
      Instant updatedAt) {
    return new User(
        id, email, username, passwordHash, fullName, phone, role, isActive, createdAt, updatedAt);
  }
}
