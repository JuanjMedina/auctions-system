package user.input;

public record RegisterUserInput(
    String email,
    String username,
    String rawPassword,
    String fullName,
    String phone,
    String role) {}
