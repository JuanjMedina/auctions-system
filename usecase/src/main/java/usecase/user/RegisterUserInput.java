package usecase.user;

import domain.user.Role;

public record RegisterUserInput(
    String email, String username, String rawPassword, String fullName, String phone, Role role) {}
