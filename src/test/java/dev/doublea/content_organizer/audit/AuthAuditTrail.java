package dev.doublea.content_organizer.audit;


import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import dev.doublea.content_organizer.model.Role;
import dev.doublea.content_organizer.model.User;
import dev.doublea.content_organizer.repository.ContentRepository;
import dev.doublea.content_organizer.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;


@SpringBootTest
@Transactional
public class AuthAuditTrail {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private static final String USERS_URL = "/api/auth/users";
    private MockMvc mockMvc;


    // The REQUIRES_NEW template is built once, after dependency injection.
    @PostConstruct
    void initTemplate() {
        requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @AfterEach
    void cleanBefore() { 
        // Native DML must execute inside a transaction: run the statements in
        // a committed (REQUIRES_NEW) block so the cleanup is effective.
        requiresNew.execute(status -> {
            AUDIT_CLEANUP_SQL.forEach(sql -> entityManager.createNativeQuery(sql).executeUpdate());
            return null;
        });
    }

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        saveUser("member", Role.MEMBER);
        saveUser("admin", Role.ADMIN);
        saveUser("adminbis", Role.ADMIN);
        saveUser("writer", Role.WRITER);
    }

    private void saveUser(String username, Role role) {
        User user = new User(username, passwordEncoder.encode("password"));
        user.setRole(role);
        userRepository.save(user);
    }

    private String createJson(String role) {
        return objectMapper.writeValueAsString(Map.of("role", role));
    }

    @Test
    void guestsCannotGetUsers() throws Exception {
        mockMvc.perform(get(USERS_URL))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void guestsCannotGetUsername() throws Exception {
        mockMvc.perform(get(USERS_URL + "/member"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username="writer", roles="WRITER")
    void writerCannotDeleteUser() throws Exception {
        mockMvc.perform(delete(USERS_URL + "/member"))
               .andExpect(status().isForbidden());
        mockMvc.perform(delete(USERS_URL + "/admin"))
           .andExpect(status().isForbidden());
        mockMvc.perform(delete(USERS_URL + "/writer"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username="member", roles="MEMBER")
    void memberCannotDeleteUser() throws Exception {
        mockMvc.perform(delete(USERS_URL + "/writer"))
               .andExpect(status().isForbidden());
        mockMvc.perform(delete(USERS_URL + "/admin"))
           .andExpect(status().isForbidden());
        mockMvc.perform(delete(USERS_URL + "/member"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void adminCanDeleteUser() throws Exception {
        mockMvc.perform(delete(USERS_URL + "/member"))
               .andExpect(status().isOk());
        mockMvc.perform(delete(USERS_URL + "/writer"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void adminCannotDeleteAdmin() throws Exception {
        mockMvc.perform(delete(USERS_URL + "/adminbis"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void adminCannotDowngradeAdminRights() throws Exception {
        mockMvc.perform(put(USERS_URL + "/adminbis")
                        .contentType("application/json")
                        .content(createJson("writer")))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void adminCanModifyRights() throws Exception {
        mockMvc.perform(put(USERS_URL + "/member")
                        .contentType("application/json")
                        .content(createJson("writer")))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username="member", roles="MEMBER")
    void memberCannotModifyRights() throws Exception {
        mockMvc.perform(put(USERS_URL + "/writer")
                        .contentType("application/json")
                        .content(createJson("member")))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username="writer", roles="WRITER")
    void writerCannotModifyRights() throws Exception {
        mockMvc.perform(put(USERS_URL + "/member")
                        .contentType("application/json")
                        .content(createJson("writer")))
               .andExpect(status().isForbidden());
        mockMvc.perform(put(USERS_URL + "/writer")
                        .contentType("application/json")
                        .content(createJson("member")))
               .andExpect(status().isForbidden());
        mockMvc.perform(put(USERS_URL + "/admin")
                        .contentType("application/json")
                        .content(createJson("writer")))
               .andExpect(status().isForbidden());
    }
}
