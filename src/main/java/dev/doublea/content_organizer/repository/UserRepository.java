package dev.doublea.content_organizer.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.doublea.content_organizer.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    
    // @Modifying is required for DELETE/UPDATE queries in Spring Data JPA.
    // Without it, Spring assumes the query returns a result set and will fail.
    // This bypasses Hibernate's normal entity lifecycle (no @PreRemove callbacks, which would usually be the case).
    @Modifying
    @Query("DELETE FROM User u WHERE u.username = :username")
    void deleteByUsername(@Param("username") String username);
}
