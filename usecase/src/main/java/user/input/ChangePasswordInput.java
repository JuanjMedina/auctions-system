package user.input;

import java.util.UUID;

public record ChangePasswordInput(UUID userId, String currentPassword, String newPassword) {}
