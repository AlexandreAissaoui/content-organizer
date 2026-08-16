package dev.doublea.content_organizer.controller;


import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import dev.doublea.content_organizer.model.Role;
import dev.doublea.content_organizer.model.User;
import dev.doublea.content_organizer.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;


/**
 * Integration tests for the authentication/authorization layer ({@code /api/auth/**}).
 *
 * <h3>Architecture</h3>
 * <ul>
 *   <li>{@code @SpringBootTest} — loads the full application context (real
 *       {@code SecurityFilterChain}, {@code JwtAuthenticationFilter}, real
 *       {@code UserRepository}).</li>
 *   <li>{@code @Transactional} — every test runs inside a transaction that is
 *       rolled back at the end, keeping the database clean between tests.</li>
 *   <li>{@code springSecurity()} — applies the real {@code SecurityFilterChain}
 *       to MockMvc so URL-based security rules are enforced.</li>
 *   <li>{@code @WithMockUser} — places a pre-built
 *       {@code UsernamePasswordAuthenticationToken} directly into the
 *       {@code SecurityContextHolder}, bypassing the real JWT login flow
 *       ({@code /api/auth/login} + {@code JwtAuthenticationFilter}).
 *       This means a bug in the JWT filter or the BCrypt matching would NOT
 *       be caught by this test class.</li>
 * </ul>
 *
 * <h3>Conventions</h3>
 * <p>User fixtures are created in {@code @BeforeEach} via {@code saveUser}
 * which encodes the password with {@code PasswordEncoder} — the only way to
 * stay consistent with the production {@code /api/auth/register} path.
 * When a test only exercises the role-change API ({@code adminCanModifyRights}),
 * the password is irrelevant and stored as a raw string for brevity.</p>
 */
@SpringBootTest
@Transactional
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String USERS_URL = "/api/auth/users";
    private MockMvc mockMvc;

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

    /**
     * Creates a user whose password is BCrypt-encoded — the same hash
     * algorithm used in production ({@code /api/auth/register}).
     * Without this, the stored password would be plain text and any
     * future test exercising the real login flow would fail silently.
     */
    private void saveUser(String username, Role role) {
        User user = new User(username, passwordEncoder.encode("password"));
        user.setRole(role);
        userRepository.save(user);
    }

    private String createJson(String role) {
        String newRole = role.toUpperCase();
        return objectMapper.writeValueAsString(Map.of("role", Role.valueOf(newRole)));
    }

    @Test
    void guestsCannotGetUsers() throws Exception {
        mockMvc.perform(get(USERS_URL))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username="member", roles="MEMBER")
    void membersCanGetUsers() throws Exception {
        mockMvc.perform(get(USERS_URL))
               .andExpect(status().isOk());
    }


    /**
     * One {@code @ParameterizedTest} = one {@code @ValueSource} entry.
     * If the assertion for "/admin" fails, the entries for "/member" and
     * "/writer" are still executed and reported — unlike a multi-assertion
     * {@code @Test} where a first failure aborts the rest.
     */
    @ParameterizedTest
    @ValueSource(strings= { "/member", "/admin", "/writer"})
    @WithMockUser(username="writer", roles="WRITER")
    void writerCannotDeleteUser(String location) throws Exception {
        mockMvc.perform(delete(USERS_URL + location))
               .andExpect(status().isForbidden());
    }

    
    @ParameterizedTest
    @ValueSource(strings= { "/member", "/admin", "/writer"})
    @WithMockUser(username="member", roles="MEMBER")
    void memberCannotDeleteUser(String location) throws Exception {
        mockMvc.perform(delete(USERS_URL + location))
               .andExpect(status().isForbidden());
        }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void unknownUserDeletionProducesNotFound() throws Exception {
        // member is not present in database
        assertThat(userRepository.findByUsername("unknown")).isEmpty();
        mockMvc.perform(delete(USERS_URL + "/unknown"))
               .andExpect(status().isNotFound());
        }

    
    @ParameterizedTest
    @ValueSource(strings= { "member", "writer"})
    @WithMockUser(username="admin", roles="ADMIN")
    void adminCanDeleteUser(String username) throws Exception {
        // member is in database
        assertThat(userRepository.findByUsername(username)).isPresent();
        mockMvc.perform(delete(USERS_URL + "/" + username))
               .andExpect(status().isOk());
        assertThat(userRepository.findByUsername(username)).isEmpty();
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

        // Arrange : create a new User
        User user = new User("test", "xxxxxxxx");
        assertThat(user.getRole()).isEqualTo(Role.MEMBER);
        userRepository.save(user);

        // Act : modify the role of said user
        mockMvc.perform(put(USERS_URL + "/test")
                        .contentType("application/json")
                        .content(createJson("writer")))
               .andExpect(status().isOk()); 

        // Asserting the role
        user = userRepository.findByUsername("test").get();
        assertThat(user.getRole()).isEqualTo(Role.WRITER);
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void cannotModifyRightsOfUnknown() throws Exception {
        // Member is not in database
        assertThat(userRepository.findByUsername("test")).isEmpty();

        // Act : modify the role of said user
        mockMvc.perform(put(USERS_URL + "/test")
                        .contentType("application/json")
                        .content(createJson("writer")))
               .andExpect(status().isNotFound()); 
    }

    @Test
    @WithMockUser(username="member", roles="MEMBER")
    void memberCannotModifyRights() throws Exception {
        mockMvc.perform(put(USERS_URL + "/writer")
                        .contentType("application/json")
                        .content(createJson("member")))
               .andExpect(status().isForbidden());
    }


    @ParameterizedTest
    @ValueSource(strings= { "member", "writer"})
    @WithMockUser(username="writer", roles="WRITER")
    void writerCannotModifyRights(String username) throws Exception {
        mockMvc.perform(put(USERS_URL + "/" + username)
                        .contentType("application/json")
                        .content(createJson("admin")))
               .andExpect(status().isForbidden());
        }
}
