package controller.dto.request;

import domain.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String username,
    @NotBlank String rawPassword,
    @NotBlank String fullName,
    String phone,
    @NotNull Role role) {}
