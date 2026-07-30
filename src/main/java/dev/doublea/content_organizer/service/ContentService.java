package dev.doublea.content_organizer.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import dev.doublea.content_organizer.config.SecurityConfig;
import dev.doublea.content_organizer.dto.content.ContentRequest;
import dev.doublea.content_organizer.dto.content.ContentResponse;
import dev.doublea.content_organizer.dto.content.ContentUpdateRequest;
import dev.doublea.content_organizer.model.Content;
import dev.doublea.content_organizer.model.Status;
import dev.doublea.content_organizer.repository.ContentRepository;

@Service
public class ContentService {

    private final ContentRepository repository;

    public ContentService(ContentRepository repository) {
        this.repository = repository;
    }

    public List<ContentResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }



    public ContentResponse findById(Integer id) {
        Content content = repository.findById(id).orElse(null);
        if (content == null) {
            SecurityConfig.getLogger().error("Content not found with id: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found with id: " + id);
        }
        return toResponse(content);
    }

    public ResponseEntity<Map<String, String>> create(ContentRequest request) {
        Content content = new Content();
        content.setTitle(request.title());
        content.setDescription(request.description());
        content.setStatus(request.status());
        content.setContentType(request.type());
        
        if (request.url() != null) {
            request.url().stream().filter(url -> ! url.isBlank()).forEach(content::addUrl);
        }

        repository.save(content);
        return ResponseEntity.created(URI.create("http://localhost/api/contents/"+content.getId())).build();
    }

    public ResponseEntity<Map<String, String>> update(Integer id, ContentUpdateRequest request) {
        if (request == null) return null;
        Content content;
        try {
            Optional<Content> checkContent = repository.findById(id);
            content = checkContent.isPresent() ? checkContent.get() : null;
        }
        catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error",e.getMessage()));
        }

        if (content == null) {
            return ResponseEntity.status(404).body(Map.of("error","Content not found"));
        }

        if (request.title() != null && !request.title().trim().isBlank()) {
            content.setTitle(request.title());
        }
        if (request.description() != null) {
            content.setDescription(request.description());
        }
        if ((request.status() != null) && (! request.status().equals(content.getStatus()))) {
            content.setStatus(request.status());
        }
        if ( request.url() != null ) {
            request.url().stream().filter(url -> ! url.isBlank()).forEach(content::addUrl);
        }
        content.setDateUpdated(LocalDateTime.now());

        repository.save(content);
        return ResponseEntity.ok().build();
        
    }

    public ResponseEntity<Map<String, String>> delete(Integer id) {
        if (!repository.existsById(id))  {
            return ResponseEntity.status(404).build();
        }
        repository.deleteById(id);
        return ResponseEntity.status(204).build();
        
    }

    public List<ContentResponse> findByTitle(String keyword) {
        return repository.findAllByTitleContainsIgnoreCase(keyword).stream().map(this::toResponse).toList();
    }

    public List<ContentResponse> findByStatus(String status) {
        // Convert the raw string input to the corresponding enum constant.
        // toUpperCase() makes the lookup case-insensitive for the caller.
        if ( status == null )
            return Collections.emptyList();
        try {
            Status newStatus = Status.valueOf(status.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            SecurityConfig.getLogger().error(e.getMessage());
            return Collections.emptyList();
        }
        Status cleanedStatus = Status.valueOf(status.toUpperCase());
        return repository.findAllByStatus(cleanedStatus).stream().map(this::toResponse).toList();
    }

    private ContentResponse toResponse(Content content) {
        if (content != null) {
            return new ContentResponse(
                    content.getId(),
                    content.getTitle(),
                    content.getDescription(),
                    content.getStatus(),
                    content.getContentType(),
                    content.getDateCreated(),
                    content.getDateUpdated(),
                    content.getSources()
            );
        }
        return null;
    }
}
