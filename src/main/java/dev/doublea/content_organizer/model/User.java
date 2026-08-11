package dev.doublea.content_organizer.model;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Audited security principal (Hibernate Envers 7.4).
 *
 * {@code @Audited(withModifiedFlag = true)} versions the user into the
 * {@code users_aud} mirror table (one row per revision, {@code REVTYPE}
 * {@code ADD} / {@code MOD} / {@code DEL}).
 *
 * <p>{@code withModifiedFlag} is justified HERE precisely because only the
 * {@code role} property is auditable:
 * <ul>
 *   <li>{@code username} is the identifier: it always appears in the audit
 *       table as the row key, but Envers generates no {@code *_MOD} column
 *       for it</li>
 *   <li>{@code password} is {@code @NotAudited}, see below;</li>
 *   <li>the result is exactly ONE meaningful flag, {@code role_MOD}, that tells
 *       whether a revision changed the role.</li>
 * </ul>
 * For entities with many audited properties (e.g. Content), the same mechanism
 * scales to one boolean per property.
 */
@Entity
@Table(name = "users")
@Audited(withModifiedFlag=true)
public class User {

    // Natural key used as the primary key instead of an auto-generated ID:
    // usernames are unique by design and are already the lookup key of the
    // whole authentication flow. Envers references this key in users_aud.
    @Id
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * {@code @NotAudited} excludes the password from the version history.
     *
     * Even though only a BCrypt hash is stored here, it is still credential
     * material: keeping it out of the {@code *_aud} tables avoids an
     * uncontrolled, permanent copy of a sensitive value in the history. Role
     * changes remain fully audited.
     */
    @NotAudited
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.role = Role.MEMBER;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(Role role) { this.role = role; }

}
