package dev.doublea.content_organizer.dto.content;

import java.util.List;

import dev.doublea.content_organizer.model.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContentUpdateRequest(
    @Size(min=1, max=127)
    String title,
    @Size(min=0, max=255)
    String description,
    @NotNull Status status,
    List<String> sources,
    List<String> authors
) {}
