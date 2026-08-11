package dev.doublea.content_organizer.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Envers extension point that decorates the global revision with the author.
 *
 * The {@code newRevision} callback is invoked exactly once per audited
 * transaction, after Envers instantiated the revision entity (through its
 * no-arg constructor, by reflection) and before the revision row is inserted.
 * It is the canonical place to enrich the revision: a JPA {@code @PrePersist}
 * callback cannot be used here (it must be a no-arg method), and a constructor
 * cannot read the runtime security context.
 *
 * The username is read from Spring Security's {@link SecurityContextHolder}, a
 * {@link ThreadLocal}. On HTTP request threads the JWT filter has already
 * populated it; on non-HTTP threads (scheduled jobs, console runners) or on
 * {@code permitAll} endpoints such as /api/auth/register it may be null or hold
 * the anonymous authentication. We degrade gracefully to "system" so that an
 * unauthenticated write is still audited instead of failing the transaction.
 */
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevisionEntity revision = (AuditRevisionEntity) revisionEntity;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ( (authentication == null) || ("anonymousUser".equals(authentication.getName())) ) {
            revision.setUsername("system");
        }
        else {
            revision.setUsername(authentication.getName());
        }
    }
}
