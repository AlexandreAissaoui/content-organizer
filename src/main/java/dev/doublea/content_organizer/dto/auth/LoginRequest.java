package dev.doublea.content_organizer.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank String username,

    @NotBlank
    @Size(min=8, max=32)
    String password
) {}
