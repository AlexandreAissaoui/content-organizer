package dev.doublea.content_organizer.dto.content;

import java.util.ArrayList;

import dev.doublea.content_organizer.model.Status;

public record ContentUpdateRequest(
    String title,
    String description,
    Status status,
    ArrayList<String> url
) {}
