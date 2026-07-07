package user.output;

import java.util.UUID;

public record UpdateUserProfileResult(
    UUID id, String email, String username, String fullName, String phone) {}
