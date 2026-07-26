package dev.doublea.content_organizer.dto.content;

import java.time.LocalDateTime;
import java.util.ArrayList;

import dev.doublea.content_organizer.model.Status;
import dev.doublea.content_organizer.model.Type;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContentRequest( 
    @NotBlank String title,
    String description,
    @NotNull Status status,
    @NotNull Type contentType,
    LocalDateTime dateCreated,
    ArrayList<String> url
) {}
