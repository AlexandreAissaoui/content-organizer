package dev.doublea.content_organizer.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

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

    public ContentResponse findById(Integer id) throws Exception {
        Content content = repository.findById(id)
                .orElseThrow(() -> new Exception("Content not found with id: " + id));
        return toResponse(content);
    }

    public void create(ContentRequest request) {
        Content content = new Content();
        content.setTitle(request.title());
        content.setDescription(request.description());
        content.setStatus(request.status());
        content.setContentType(request.contentType());
        
        if (request.url() != null) {
            request.url().stream().filter(url -> ! url.isBlank()).forEach(content::addUrl);
        }

        content.setDateCreated(request.dateCreated());
        repository.save(content);
    }

    public void update(Integer id, ContentUpdateRequest request) throws Exception {
        Content content = repository.findById(id)
                .orElseThrow(() -> new Exception("Content not found with id: " + id));

        if (request.title() != null && !request.title().isBlank()) {
            content.setTitle(request.title());
        }
        if (request.description() != null) {
            content.setDescription(request.description());
        }
        if (request.status() != null) {
            content.setStatus(request.status());
        }
        if ( request.url() != null ) {
            request.url().stream().filter(url -> ! url.isBlank()).forEach(content::addUrl);
        }
        content.setDateUpdated(LocalDateTime.now());

        repository.save(content);
    }

    public void delete(Integer id) throws Exception {
        if (!repository.existsById(id))  {
            throw new Exception("Content not found with id: " + id);
        }
        repository.deleteById(id);
    }

    public List<ContentResponse> findByTitle(String keyword) {
        return repository.findAllByTitleContainsIgnoreCase(keyword).stream().map(this::toResponse).toList();
    }

    public List<ContentResponse> findByStatus(String status) {
        // Convert the raw string input to the corresponding enum constant.
        // toUpperCase() makes the lookup case-insensitive for the caller.
        Status cleanedStatus = Status.valueOf(status.toUpperCase());
        return repository.findAllByStatus(cleanedStatus).stream().map(this::toResponse).toList();
    }

    private ContentResponse toResponse(Content content) {
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
}
