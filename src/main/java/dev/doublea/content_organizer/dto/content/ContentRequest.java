package dev.doublea.content_organizer.dto.content;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.doublea.content_organizer.model.Status;
import dev.doublea.content_organizer.model.Type;
import jakarta.validation.constraints.NotNull;

public record ContentRequest( 
    String title,
    String description,
    @NotNull Status status,
    @NotNull Type type,
    @JsonIgnore LocalDateTime dateCreated,
    ArrayList<String> url
) {}
