package dev.doublea.content_organizer.model;

import java.time.LocalDateTime;
import java.util.ArrayList;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotBlank(message = "Empty title is prohibited")
    private String title;
    private String description;

    // Stores the enum as its name string in the DB
    // This avoids data corruption if enum values are reordered or removed.
    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private Type contentType;

    // Prevents Hibernate from updating this column after the entity is persisted.
    // The creation timestamp is set once and should never change.
    @Column(updatable = false)
    private LocalDateTime dateCreated;
    private LocalDateTime dateUpdated;
    private final ArrayList<String> sources = new ArrayList<>();

    public Content() {}

    public Content(Integer id) { this.id = id; }

    public Content(Integer id,
    String title,
    String description,
    Status status,
    Type contentType,
    String url) {
        this.id=id;
        this.title=title;
        this.description=description;
        this.status=status;
        this.contentType=contentType;
        this.dateCreated = dateCreated != null ? dateCreated : LocalDateTime.now();
        if (url != null) 
            sources.add(url);
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public Type getContentType() { return contentType; }
    public LocalDateTime getDateCreated() { return dateCreated; }
    public LocalDateTime getDateUpdated() { return dateUpdated; }
    public ArrayList<String> getSources() { return new ArrayList<>(sources); }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(Status status) { this.status = status; }
    public void setContentType(Type contentType) { this.contentType = contentType; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }
    public void setDateUpdated(LocalDateTime dateUpdated) { this.dateUpdated = dateUpdated; }
    public void addUrl(String url) { sources.add(url); }
}
