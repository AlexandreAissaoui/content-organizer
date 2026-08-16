package dev.doublea.content_organizer.dto.content;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.doublea.content_organizer.model.Status;
import dev.doublea.content_organizer.model.Type;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContentRequest(
    @NotBlank 
    @Size(min=1, max=127)
    String title,
    @Size(min=0, max=255)
    String description,
    @NotNull Status status,
    @NotNull Type type,
    @JsonIgnore LocalDateTime dateCreated,
    List<String> sources,
    List<String> authors
) {}
