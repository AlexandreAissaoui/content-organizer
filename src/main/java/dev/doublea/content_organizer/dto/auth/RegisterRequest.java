package dev.doublea.content_organizer.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank
    @Size(min=1, max=15)
    String username,

    @NotBlank
    @Size(min=8, max=32)
    String password
) {}
