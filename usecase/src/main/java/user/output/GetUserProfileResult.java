package user.output;

import domain.user.Role;
import java.time.Instant;
import java.util.UUID;

public record GetUserProfileResult(
    UUID id,
    String email,
    String username,
    String fullName,
    String phone,
    Role role,
    Instant createdAt) {}
