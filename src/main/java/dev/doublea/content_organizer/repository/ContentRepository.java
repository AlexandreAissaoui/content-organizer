package dev.doublea.content_organizer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.doublea.content_organizer.model.Content;
import dev.doublea.content_organizer.model.Status;

public interface ContentRepository extends JpaRepository<Content,Integer> {

    // Implicit JPQL query : case-insensitive substring search.
    List<Content> findAllByTitleContainsIgnoreCase(String keyword);
    List<Content> findAllByStatus(Status status);
    List<Content> findAllBySourcesIn(List<String> sources);


}
