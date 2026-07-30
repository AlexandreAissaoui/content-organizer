package dev.doublea.content_organizer.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<Map<String, String>> create(@Valid @RequestBody ContentRequest request) {
        return contentService.create(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> update(@Valid @RequestBody ContentUpdateRequest request, @PathVariable Integer id) {
        return contentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        return contentService.delete(id);
    }

    @GetMapping("/filter/{keyword}")
    public List<ContentResponse> findByTitle(@PathVariable String keyword) {
        return contentService.findByTitle(keyword);
    }

    @GetMapping("/filter/status/{status}")
    public List<ContentResponse> findByStatus(@PathVariable String status) {
        return contentService.findByStatus(status);
    }
}