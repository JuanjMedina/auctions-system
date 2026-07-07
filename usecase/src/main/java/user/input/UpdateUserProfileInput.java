package user.input;

import java.util.UUID;

public record UpdateUserProfileInput(
    UUID userId, String email, String username, String fullName, String phone) {}
