package dev.doublea.content_organizer.dto.auth;

import dev.doublea.content_organizer.model.Role;
import jakarta.validation.constraints.NotNull;

public record ModifyRequest(@NotNull Role role) {}