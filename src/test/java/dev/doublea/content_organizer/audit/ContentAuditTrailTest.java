package dev.doublea.content_organizer.audit;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import dev.doublea.content_organizer.model.Content;
import dev.doublea.content_organizer.model.Status;
import dev.doublea.content_organizer.model.Type;
import dev.doublea.content_organizer.repository.ContentRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;

/**
 * Integration tests for the Hibernate Envers 7.4 audit trail of Content.
 *
 * <h2>Transaction strategy (the heart of this class)</h2>
 * <ol>
 *   <li>Envers does not write the audit rows at flush time: it defers them to
 *       the <strong>commit</strong> of the enclosing transaction. A transaction
 *       that rolls back therefore leaves no trace in the {@code *_aud} tables
 *       nor in {@code audit_revision_entity}.</li>
 *   <li>The test class is {@code @Transactional}: the test method always rolls
 *       back at the end. Every audited mutation must therefore run inside its
 *       own <em>committed</em> transaction, obtained with
 *       {@link TransactionTemplate} and {@code PROPAGATION_REQUIRES_NEW}:
 *       REQUIRES_NEW suspends the test transaction, opens a fresh one that
 *       commits independently, then resumes the test transaction. This is the
 *       deliberate "commit point" that makes the audit durable and readable.</li>
 *   <li>{@code @Transactional} stays on the class so the injected
 *       {@link EntityManager} proxy remains bound to one open transaction for
 *       the whole test method. Without it, each call opens and closes its own
 *       session and the {@link AuditReader} fails with "The associated entity
 *       manager is closed".</li>
 * </ol>
 *
 * <h2>Why @WithMockUser works inside REQUIRES_NEW</h2>
 * {@link SecurityContextHolder} is a {@link ThreadLocal}. REQUIRES_NEW keeps
 * the <em>same thread</em>, so the mock authentication installed by
 * {@code @WithMockUser} is visible to {@link AuditRevisionListener} inside the
 * nested transaction.
 */
@SpringBootTest
@Transactional
class ContentAuditTrailTest {

    /**
     * Cleanup statements in child-to-parent order (FK constraints):
     * <ol>
     *   <li>the {@code *_aud} mirror tables first (they reference content and
     *       the global revision table);</li>
     *   <li>then the {@code @ElementCollection} join tables;</li>
     *   <li>then {@code content} itself;</li>
     *   <li>then the global revision table;</li>
     *   <li>finally the revision-id sequence is RESTARTed so that every test
     *       sees deterministic revision numbers starting at 1.</li>
     * </ol>
     */
    private static final List<String> AUDIT_CLEANUP_SQL = List.of(
        "DELETE FROM content_authors_aud",
        "DELETE FROM content_sources_aud",
        "DELETE FROM content_aud",
        "DELETE FROM users_aud",
        "DELETE FROM content_authors",
        "DELETE FROM content_sources",
        "DELETE FROM content",
        "DELETE FROM users",
        "DELETE FROM audit_revision_entity",
        "ALTER SEQUENCE audit_revision_entity_seq RESTART WITH 1"
    );

    @Autowired 
    private ContentRepository contentRepository;
    
    @Autowired 
    private EntityManager entityManager;                                          
    
    @Autowired 
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate requiresNew;

    // The REQUIRES_NEW template is built once, after dependency injection.
    @PostConstruct
    void initTemplate() {
        requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Cleanup runs in {@code @BeforeEach} -- never in {@code @AfterEach}.
     *
     * The next test's {@code @BeforeEach} removes the committed leftovers 
     * of the previous one.
     */
    
    @AfterEach
    void cleanBefore() { 
        // Native DML must execute inside a transaction: run the statements in
        // a committed (REQUIRES_NEW) block so the cleanup is effective.
        requiresNew.execute(status -> {
            AUDIT_CLEANUP_SQL.forEach(sql -> entityManager.createNativeQuery(sql).executeUpdate());
            return null;
        });
    }

    private AuditReader auditReader() {
        // An AuditReader is a lightweight read-only view over the current
        // EntityManager (AuditReaderFactory.get() never opens a session itself).
        return AuditReaderFactory.get(entityManager);
    }

    /**
     * Inserts and COMMITS a Content inside REQUIRES_NEW. This is the only way
     * to make Envers persist the ADD revision: the write is committed
     * independently of the test transaction, which will roll back.
     *
     * Note the returned entity is detached (its transaction is over).
     */
    private Content savedContent() {
        return requiresNew.execute( status -> { 
            Content content = new Content();
            content.setTitle("Audited content");
            content.setStatus(Status.IDEA);
            content.setType(Type.ARTICLE);
            content.addAuthor("admin");
            return contentRepository.saveAndFlush(content); 
        });
    }

    /**
     * Envers 7.x removed {@code AuditReader.getRevisionType(Class, id, rev)}.
     *
     * The query API equivalent: project the {@code REVTYPE} column for a given
     * entity id and revision number.
     * {@code forRevisionsOfEntity(Content.class, false, true)} returns revision
     * metadata (not the entity snapshots) and includes deleted rows, which the
     * DEL test relies on.
     */
    private RevisionType revisionTypeOf(Integer entityId, Number revisionNumber) {
        return (RevisionType) auditReader().createQuery()
            .forRevisionsOfEntity(Content.class, false, true)
            .addProjection(AuditEntity.revisionType())
            .add(AuditEntity.id().eq(entityId))
            .add(AuditEntity.revisionNumber().eq(revisionNumber))
            .getSingleResult();
    }

    
    @Test
    @WithMockUser(username = "admin")
    void createProducesOneAddRevision() {
        Content content = savedContent(); // committed ADD revision

        // getRevisions() returns, in order, every revision number of the entity.
        List<Number> revisions = auditReader().getRevisions(Content.class, content.getId());

        assertThat(revisions).hasSize(1); // exactly one revision for a single create
        RevisionType lastType = revisionTypeOf(content.getId(), revisions.get(0));
        assertThat(lastType).isEqualTo(RevisionType.ADD); // REVTYPE of that revision is ADD
    }


    @Test
    @WithMockUser(username = "admin")
    void updateProducesOneModificationRevision() {
        Content content = savedContent();
        // ENVERS RULE: the MOD revision is only persisted when this update
        // COMMITS (Envers defers the audit to the commit). REQUIRES_NEW gives
        // the mutation its own committed transaction; inside the rolling-back
        // test transaction it would leave no audit row. The entity is re-fetched
        // within the new transaction because `content` is now detached.
        requiresNew.execute( status -> {
            Content found = contentRepository.findById(content.getId()).orElseThrow();
            found.setStatus(Status.PUBLISHED);
            contentRepository.saveAndFlush(found);
            return null;
        });
        List<Number> revisions = auditReader().getRevisions(Content.class, content.getId());
        assertThat(revisions).hasSize(2); // one ADD + one MOD
        
        Number revisionNumber = auditReader().getRevisions(Content.class, content.getId()).get(1);
        RevisionType lastType = revisionTypeOf(content.getId(), revisionNumber);

        assertThat(lastType).isEqualTo(RevisionType.MOD);
    }

    

    @Test
    @WithMockUser(username = "admin")
    void deleteProducesOneDeleteRevision() {
        Content content = savedContent();
        // Same commit rule as the update test: the DEL revision only appears at
        // COMMIT, so the delete runs in its own committed REQUIRES_NEW block.
        requiresNew.execute( status -> {
            contentRepository.delete(content);
            contentRepository.flush();
            return null;
        });

        List<Number> revisions = auditReader().getRevisions(Content.class, content.getId());

        assertThat(revisions).hasSize(2); // one ADD + one DEL
        RevisionType lastType = revisionTypeOf(content.getId(), revisions.get(1));

        assertThat(lastType).isEqualTo(RevisionType.DEL);
    }


    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void revisionIsAttributedToTheAuthenticatedUser() {
        Content content = savedContent(); // committed ADD, author resolved at commit

        Number revisionNumber = auditReader().getRevisions(Content.class, content.getId()).get(0);
        // findRevision() materializes the global revision row (audit_revision_entity).
        AuditRevisionEntity revision = auditReader().findRevision(AuditRevisionEntity.class, revisionNumber);
        assertThat(revisionNumber).isEqualTo(1); // deterministic thanks to the sequence reset
        // The listener must have captured the mocked principal running this thread.
        assertThat(revision.getUsername()).isEqualTo("admin");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void revisionIsNotModifiable() {
        Content content = savedContent(); // committed ADD, author resolved at commit


        Number revisionNumber = auditReader().getRevisions(Content.class, content.getId()).get(0);
        // findRevision() materializes the global revision row (audit_revision_entity).
        //auditReader().createQuery().forEntitiesAtRevision(Content.class, revisionNumber).
        AuditRevisionEntity revision = auditReader().findRevision(AuditRevisionEntity.class, revisionNumber);
        assertThat(revisionNumber).isEqualTo(1); // deterministic thanks to the sequence reset
        // The listener must have captured the mocked principal running this thread.
        assertThat(revision.getUsername()).isEqualTo("admin");
    }
}
