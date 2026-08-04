package dev.doublea.content_organizer.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

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
    private Type type;

    // Prevents Hibernate from updating this column after the entity is persisted.
    // The creation timestamp is set once and should never change.
    @Column(updatable = false)
    private LocalDateTime dateCreated;
    
    private LocalDateTime dateUpdated;
    
    @ElementCollection
    @CollectionTable(
        name = "content_sources",
        joinColumns = @JoinColumn(name = "content_id")
    )
    @Column(name = "source", nullable = false)
    private final List<String> sources = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
        name = "content_authors",
        joinColumns = @JoinColumn(name = "content_id")
    )
    @Column(name = "author", nullable = false)
    @NotEmpty private final List<String> authors = new ArrayList<>();
    
    
    // For JPA, a no-argument constructor is needed
    protected Content() {}

    public Content(String title) { this.title = title; }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public Type getType() { return type; }
    public LocalDateTime getDateCreated() { return dateCreated; }
    public LocalDateTime getDateUpdated() { return dateUpdated; }
    public List<String> getSources() { return new ArrayList<>(sources); }
    public List<String> getAuthors() { return new ArrayList<>(authors); }

    @PrePersist
    public void prePersist() {
        if (dateCreated == null) dateCreated = LocalDateTime.now();
}

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(Status status) { this.status = status; }
    public void setType(Type type) { this.type = type; }
    public void setDateUpdated(LocalDateTime dateUpdated) { this.dateUpdated = dateUpdated; }
    public void addSource(String url) { if ( ! sources.contains(url)) { sources.add(url); } }
    public void addAuthor(String user) { if ( ! authors.contains(user)) { authors.add(user); } }
}
