package dev.doublea.content_organizer.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank String username,
    @NotBlank String password
) {}
