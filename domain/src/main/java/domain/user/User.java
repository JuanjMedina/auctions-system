package domain.user;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class User {
  private final UUID id;

  private String email;
  private String username;
  private String passwordHash;
  private String fullName;

  private Role role;
  private boolean isActive;

  private final Instant createdAt;
  private Instant updatedAt;
}
