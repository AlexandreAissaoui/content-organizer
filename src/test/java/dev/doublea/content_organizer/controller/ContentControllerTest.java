package dev.doublea.content_organizer.controller;

import java.util.List;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import dev.doublea.content_organizer.dto.content.ContentRequest;
import dev.doublea.content_organizer.model.Content;
import dev.doublea.content_organizer.model.Role;
import dev.doublea.content_organizer.model.Status;
import dev.doublea.content_organizer.model.Type;
import dev.doublea.content_organizer.model.User;
import dev.doublea.content_organizer.repository.ContentRepository;
import dev.doublea.content_organizer.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;


@SpringBootTest
@Transactional
public class ContentControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String CONTENTS_URL = "/api/contents";
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        saveUser("member", Role.MEMBER);
        saveUser("admin", Role.ADMIN);
    }

    private void saveUser(String username, Role role) {
        User user = new User(username, passwordEncoder.encode("password"));
        user.setRole(role);
        userRepository.save(user);
    }

    String createJson() {
        return objectMapper.writeValueAsString(new ContentRequest(
                "New Content",
                "Test",
                Status.IDEA,
                Type.ARTICLE,
                null,
                List.of("https://example.com"),
                List.of("admin1")
            ));
    }

    /*
    Belongs to AuthControllerTest.java 
    @Test
    void createUser(String username, String password, String role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", username,
                    "password", password))))
                .andExpect(status().isCreated());
    }
    */

    @Test
    void shouldRejectUnauthenticatedUsers() throws Exception {
        mockMvc.perform(get(CONTENTS_URL))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void usersCanGetContents() throws Exception {
        mockMvc.perform(get(CONTENTS_URL))
               .andExpect(status().isOk());
    }

    @Test
    void guestsCannotCreateContents() throws Exception {
        mockMvc.perform(post(CONTENTS_URL)
                .contentType("application/json")
                .content(createJson()))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void membersCannotCreateContent() throws Exception {
        mockMvc.perform(post(CONTENTS_URL)
                .contentType("application/json")
                .content(createJson()))
               .andExpect(status().isForbidden());
    }

/*
    @Test
    @WithMockUser(roles = "WRITER")
    void writersCanCreateContent() throws Exception {
        mockMvc.perform(post(CONTENTS_URL)
                .contentType("application/json")
                .content("{\"title\":\"New Content\",\"description\":\"Test\",\"type\":\"ARTICLE\",\"status\":\"IDEA\",\"url\":[\"https://example.com\"]}"))
               .andExpect(status().isOk());
    }
*/
    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void membersCannotDeleteContent() throws Exception {
        mockMvc.perform(delete(CONTENTS_URL+"/1"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanCreateContent() throws Exception {
        mockMvc.perform(post(CONTENTS_URL)
                .contentType("application/json")
                .content(createJson()))
               .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanDeleteContent() throws Exception {
        // Arrange : create an existing content in the database
        Content content = new Content();
        content.setTitle("To Delete");
        content.setStatus(Status.IDEA);
        content.setType(Type.ARTICLE);
        content.addAuthor("admin");
        Integer savedId = contentRepository.save(content).getId();

        // Act : delete this content via the API
        mockMvc.perform(delete(CONTENTS_URL + "/" + savedId))

        // Assert : the deletion succeeds without a response body
               .andExpect(status().isNoContent());
    }
}
