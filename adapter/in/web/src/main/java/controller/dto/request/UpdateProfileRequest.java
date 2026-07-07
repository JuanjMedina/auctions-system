package controller.dto.request;

import jakarta.validation.constraints.Email;

public record UpdateProfileRequest(
    @Email String email, String username, String fullName, String phone) {}
