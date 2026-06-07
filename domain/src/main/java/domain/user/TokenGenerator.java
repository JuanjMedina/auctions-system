package domain.user;

import java.util.UUID;

public interface TokenGenerator {
  String generateAccessToken(UUID userId, String email, Role role);

  String generateRefreshToken(UUID userId);

  UUID extractUserIdFromRefreshToken(String refreshToken);
}
