package dev.doublea.content_organizer.dto.content;

import java.util.List;

import dev.doublea.content_organizer.model.Status;
import jakarta.validation.constraints.NotNull;

public record ContentUpdateRequest(
    String title,
    String description,
    @NotNull Status status,
    List<String> sources,
    List<String> authors
) {}
