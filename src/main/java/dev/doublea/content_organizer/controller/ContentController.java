package dev.doublea.content_organizer.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.doublea.content_organizer.dto.content.ContentRequest;
import dev.doublea.content_organizer.dto.content.ContentResponse;
import dev.doublea.content_organizer.dto.content.ContentUpdateRequest;
import dev.doublea.content_organizer.service.ContentService;
import jakarta.validation.Valid;



@RestController
@RequestMapping("/api/contents")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public List<ContentResponse> findAll() {
        return contentService.findAll();
    }

    @GetMapping("/{id}")
    public ContentResponse findById(@PathVariable Integer id) {
        return contentService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@Valid @RequestBody ContentRequest request, Authentication authentication) {
        return contentService.create(request, authentication);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> update(@Valid @RequestBody ContentUpdateRequest request, @PathVariable Integer id, Authentication authentication) {
        return contentService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id, Authentication authentication) {
        return contentService.delete(id, authentication);
    }

    @GetMapping("/filter/{keyword}")
    public List<ContentResponse> findByTitle(@PathVariable String keyword) {
        return contentService.findByTitle(keyword);
    }

    @GetMapping("/filter/status/{status}")
    public List<ContentResponse> findByStatus(@PathVariable String status) {
        return contentService.findByStatus(status);
    }

    @GetMapping("/filter/sources/{sources}")
    public List<ContentResponse> findBySource(@PathVariable List<String> sources) {
        return contentService.findBySources(sources);
    }
}