package dev.doublea.content_organizer.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.Audited;

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

/**
 * Audited aggregate root (Hibernate Envers 7.4).
 *
 * {@code @Audited(withModifiedFlag = true)} versions every audited property of
 * this entity:
 * <ul>
 *   <li>a mirror table {@code content_aud} is created: one row per revision of
 *       the entity, keyed by {@code (id, REV)} with a {@code REVTYPE} column
 *       ({@code ADD} / {@code MOD} / {@code DEL});</li>
 *   <li>because {@code withModifiedFlag} is true, one boolean {@code *_MOD}
 *       column is added per audited property, telling which fields actually
 *       changed in that revision;</li>
 *   <li>rows are never updated in place: each change appends a new revision
 *       row, so the full history of the entity remains queryable.</li>
 * </ul>
 */
@Entity
@Audited(withModifiedFlag=true)
public class Content {
    /**
     * IDENTITY delegates the id generation to the database
     * ({@code content_id_seq} on PostgreSQL). Envers references this id
     * unchanged inside the audit tables, as the business key of the history.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @NotBlank(message = "Empty title is prohibited")
    private String title;

    private String description;

    // The enum is stored as its name (STRING), so reordering or removing enum
    // constants never corrupts existing rows. Audit columns follow the same
    // convention.
    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private Type type;

    // Written once at persist time and never updated afterwards (updatable = false).
    @Column(updatable = false)
    private LocalDateTime dateCreated;
    
    private LocalDateTime dateUpdated;
    
    /**
     * @ElementCollection collections are versioned into dedicated mirror tables
     * ({@code content_sources_aud}, {@code content_authors_aud}). Envers audits
     * them element by element: each row carries the {@code (content_id, REV,
     * REVTYPE)} key, so adding or removing a single source yields its own
     * audited entry instead of rewriting the whole collection.
     */
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
    
    
    // JPA requires a no-argument constructor for entity instantiation
    // (Envers itself re-instantiates audited entities through reflection).
    public Content() {}

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
    public void addSource(String url) { if ( (! sources.contains(url)) && (sources.size() < 20) ) { sources.add(url); } }
    public void addAuthor(String user) { if ( (! authors.contains(user)) && (authors.size() < 20) ) { authors.add(user); } }
}
