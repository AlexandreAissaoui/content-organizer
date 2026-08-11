package dev.doublea.content_organizer.audit;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

/**
 * Global revision entity (Hibernate Envers 7.4).
 *
 * Envers does not store one timestamp per audited row: every audited
 * transaction produces exactly ONE row in this table -- the "revision" (REV) --
 * and all the *_aud rows written by that transaction reference it. This design
 * keeps the history compact and makes it possible to attribute a whole batch of
 * changes to a single author and instant.
 *
 * Mapping contract:
 * <ul>
 *   <li>{@link RevisionNumber} : the revision id, exposed as the {@code REV}
 *       column of every {@code *_aud} table (foreign key to this entity).</li>
 *   <li>{@link RevisionTimestamp} : revision time as a {@code long}
 *       (epoch milliseconds), populated at <strong>commit</strong> time -- not
 *       when the business transaction started.</li>
 *   <li>{@link RevisionEntity}: registers the {@link AuditRevisionListener}
 *       invoked once per audited transaction, right before the revision row is
 *       inserted.</li>
 * </ul>
 */
@Entity
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevisionEntity {

    /**
     * The revision number, shared by every audit row of the transaction.
     *
     * The id comes from the {@code audit_revision_entity_seq} sequence. Two
     * design decisions, both important:
     * <ul>
     *   <li><strong>Sequence strategy</strong> ({@link GenerationType#SEQUENCE})
     *       instead of AUTO, so the mapping is explicit and stable.</li>
     *   <li><strong>{@code allocationSize = 1}</strong>: each id is fetched
     *       straight from the database ({@code nextval}), disabling the pooled
     *       in-memory optimizer that Hibernate would otherwise use for
     *       {@code allocationSize = 50}. The pooled optimizer keeps a JVM-wide
     *       generation state; when a test resets the sequence with
     *       {@code RESTART WITH 1}, that stale state makes the next refill
     *       compute {@code nextval - 49}, i.e. a negative revision number.
     *       {@code allocationSize = 1} removes the pool, so revision numbers
     *       always follow the sequence and tests are deterministic.</li>
     * </ul>
     */
    @Id
    @SequenceGenerator(name = "audit_revision_entity_seq_gen", sequenceName = "audit_revision_entity_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_revision_entity_seq_gen")
    @RevisionNumber
    private int id;

    /**
     * Commit time of the audited transaction, in epoch milliseconds
     * (written by Envers at commit, see class Javadoc).
     */
    @RevisionTimestamp
    private long timestamp;

    /**
     * Author of the audited transaction, resolved by the revision listener from
     * the Spring Security context.
     *
     * Kept effectively immutable (updatable=false and the first-write-wins
     * guard in the setter): a transaction has a single author, so a later
     * invocation in the same transaction must not overwrite it.
     */
    @Column(name = "username", updatable = false)
    private String username = null;

    public int getId() { return id; }

    public long getTimestamp() { return timestamp; }

    public String getUsername() { return username; }

    public void setUsername(String username) { if (this.username == null) { this.username = username; } }
}
