package domain.user;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = lombok.AccessLevel.PRIVATE)
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

  public static User create(
      String email,
      String username,
      String passwordHash,
      String fullName,
      String phone,
      Role role) {
    Instant now = Instant.now();
    return User.builder()
        .id(UUID.randomUUID())
        .email(email)
        .username(username)
        .passwordHash(passwordHash)
        .fullName(fullName)
        .phone(phone)
        .role(role)
        .isActive(true)
        .createdAt(now)
        .updatedAt(now)
        .build();
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
    return User.builder()
        .id(id)
        .email(email)
        .username(username)
        .passwordHash(passwordHash)
        .fullName(fullName)
        .phone(phone)
        .role(role)
        .isActive(isActive)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }
}
