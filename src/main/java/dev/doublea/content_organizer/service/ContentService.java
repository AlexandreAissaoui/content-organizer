package dev.doublea.content_organizer.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import dev.doublea.content_organizer.dto.content.ContentRequest;
import dev.doublea.content_organizer.dto.content.ContentResponse;
import dev.doublea.content_organizer.dto.content.ContentUpdateRequest;
import dev.doublea.content_organizer.model.Content;
import dev.doublea.content_organizer.model.Role;
import dev.doublea.content_organizer.model.Status;
import dev.doublea.content_organizer.model.User;
import dev.doublea.content_organizer.repository.ContentRepository;
import dev.doublea.content_organizer.repository.UserRepository;

@Service
public class ContentService {

    private final ContentRepository repository;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(ContentService.class);

    private static final  String ERROR = "error";
    private static final  String CONTENT_NOT_FOUND = "Content not found";
    private static final String USER_NOT_FOUND = "User not found";

    public ContentService(ContentRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    // Check rights of the user to update or delete content
    private boolean existsWriter(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            return userOptional.get().getRole().equals(Role.WRITER) || userOptional.get().getRole().equals(Role.ADMIN);
        }
        return false;
    }

    public List<ContentResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public ContentResponse findById(Integer id) {
        Content content = repository.findById(id).orElse(null);
        if (content == null) {
            logger.error("\nContent not found with id: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found with id: " + id);
        }
        return toResponse(content);
    }

    public ResponseEntity<Map<String, String>> create(ContentRequest request, Authentication authentication) {
        if (authentication == null ) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_FOUND);
        }
        if (request == null) {
            return ResponseEntity.status(400).body(Map.of(ERROR, "Invalid request"));
        }
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(ERROR, USER_NOT_FOUND));    
        }
        // Only an admin or a writer can create content
        if ((! userOpt.get().getRole().equals(Role.ADMIN)) && (! userOpt.get().getRole().equals(Role.WRITER)) ) {
            return ResponseEntity.status(403).body(Map.of(ERROR, "Not enough rights to create content"));
        }

        Content content = new Content();
        content.addAuthor(username);
        content.setTitle(request.title());
        content.setDescription(request.description());
        content.setStatus(request.status());
        content.setType(request.type());

        if (request.sources() != null) {
            // No blank sources and no duplicates are allowed
            Set<String> uniqueSources = new HashSet<>(request.sources());
            uniqueSources.stream().filter(url -> ! url.isBlank() ).forEach(content::addSource);
        }

        repository.save(content);
        return ResponseEntity.created(URI.create("http://localhost:8080/api/contents/"+content.getId())).body(Map.of("success", "Content created with id: " +content.getId()));
    }

    public ResponseEntity<Map<String, String>> update(Integer id, ContentUpdateRequest request, Authentication authentication) {
        if (request == null) {
            return ResponseEntity.status(400).body(Map.of(ERROR, "Invalid request"));
        } 
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_FOUND);
        }
        Optional<Content> checkContent = repository.findById(id);
        Content content = checkContent.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, CONTENT_NOT_FOUND));
        
        Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(ERROR,USER_NOT_FOUND));    
        }
        // Only an admin or a writer of the shared content can update it
        if (userOpt.get().getRole().equals(Role.ADMIN) || content.getAuthors().contains(authentication.getName())) {
            if (request.title() != null && !request.title().trim().isBlank()) {
            content.setTitle(request.title());
            }
            if (request.description() != null) {
                content.setDescription(request.description());
            }
            if ((! request.status().equals(content.getStatus()))) {
                content.setStatus(request.status());
            }
            if ( request.sources() != null ) {
                Set<String> uniqueSources = new HashSet<>(request.sources());

                uniqueSources.stream().filter(url -> ! url.isBlank()).forEach(content::addSource);
            }
            if (request.authors() != null && ! request.authors().isEmpty()) {
                Set<String> uniqueAuthors = new HashSet<>(request.authors());
                uniqueAuthors.stream().filter(this::existsWriter).forEach(content::addAuthor);
            }
            content.setDateUpdated(LocalDateTime.now());

            repository.save(content);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(403).body(Map.of(ERROR, "Not enough rights to update content"));
    }

    public ResponseEntity<Map<String, String>> delete(Integer id, Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_FOUND);
        }
        if (!repository.existsById(id))  {
            return ResponseEntity.status(404).body(Map.of(ERROR,CONTENT_NOT_FOUND));
        }
        Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(ERROR,USER_NOT_FOUND));    
        }
        if (userOpt.get().getRole().equals(Role.ADMIN)) {
            repository.deleteById(id);
            return ResponseEntity.status(204).build();
        }
        return ResponseEntity.status(403).body(Map.of(ERROR,"Not enough rights to delete content"));
        
    }

    public List<ContentResponse> findByTitle(String keyword) {
        return repository.findAllByTitleContainsIgnoreCase(keyword).stream().map(this::toResponse).toList();
    }

    public List<ContentResponse> findByStatus(String status) {
        // Convert the raw string input to the corresponding enum constant.
        // toUpperCase() makes the lookup case-insensitive for the caller.
        if ( status == null )
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is missing");
        Status newStatus;
        try {
            newStatus = Status.valueOf(status.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status");
        }
        return repository.findAllByStatus(newStatus).stream().map(this::toResponse).toList();
    }

    private ContentResponse toResponse(Content content) {
        if (content != null) {
            return new ContentResponse(
                    content.getId(),
                    content.getTitle(),
                    content.getDescription(),
                    content.getStatus(),
                    content.getType(),
                    content.getDateCreated(),
                    content.getDateUpdated(),
                    content.getSources(),
                    content.getAuthors()
            );
        }
        return null;
    }

    public List<ContentResponse> findBySources(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            return Collections.emptyList();
        }
        return repository.findAllBySourcesIn(sources).stream().map(this::toResponse).toList();
    }
}
