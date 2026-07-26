package dev.doublea.content_organizer.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/content")
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
        try {
            return contentService.findById(id);
        }
        catch (Exception e) {
            System.out.println("Error searching content "+id);
            return null;
        }
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public void create(@Valid @RequestBody ContentRequest request) {
        contentService.create(request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/{id}")
    public void update(@Valid @RequestBody ContentUpdateRequest request, @PathVariable Integer id) {
        try {
            contentService.update(id, request);
        }
        catch (Exception e) {
            System.out.println("Error updating content");
        }
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        try {
            contentService.delete(id);
        }
        catch (Exception e) {
            System.out.println("Error searching content "+id);
        }
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