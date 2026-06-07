package user.output;

import java.util.UUID;

public record RegisterUserResult(UUID userId, String email, String username, UUID walletId) {}
