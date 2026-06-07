package usecase.user;

import domain.user.Role;

public record RegisterUserCommand(
    String email, String username, String rawPassword, String fullName, String phone, Role role) {}
