package dev.doublea.content_organizer.controller;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import dev.doublea.content_organizer.dto.content.ContentRequest;
import dev.doublea.content_organizer.dto.content.ContentUpdateRequest;
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
        saveUser("writer", Role.WRITER);
    }

    private void saveUser(String username, Role role) {
        User user = new User(username, passwordEncoder.encode("password"));
        user.setRole(role);
        userRepository.save(user);
    }

    private String createJson(String participantName, String title) {
        return objectMapper.writeValueAsString(new ContentRequest(
                title,
                "Test",
                Status.IDEA,
                Type.ARTICLE,
                null,
                List.of("https://example.com"),
                List.of(participantName)
            ));
    }

    private String modifyJson(String title) {
        return objectMapper.writeValueAsString(new ContentUpdateRequest(
            title ,"",Status.IDEA, Collections.emptyList(), List.of("admin")));
    }

    private Content createContent(String title, String author) {
        Content content = new Content(title);
        content.setStatus(Status.IDEA);
        content.setType(Type.ARTICLE);
        content.addAuthor(author);
        return content;
    }

    @Test
    void shouldRejectUnauthenticatedUsers() throws Exception {
        mockMvc.perform(get(CONTENTS_URL))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void guestsCannotCreateContents() throws Exception {
        mockMvc.perform(post(CONTENTS_URL)
                .contentType("application/json")
                .content(createJson("", "New Content")))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void guestsCannotModifyContent() throws Exception {
        mockMvc.perform(put(CONTENTS_URL + "/1")
                        .contentType("application/json")
                        .content(modifyJson("New Content")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guestsCannotDeleteContent() throws Exception {
        mockMvc.perform(delete(CONTENTS_URL + "/1" ))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void usersCanGetContents() throws Exception {
        mockMvc.perform(get(CONTENTS_URL))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void membersCannotCreateContent() throws Exception {
        mockMvc.perform(post(CONTENTS_URL)
                .contentType("application/json")
                .content(createJson("member", "New Content")))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void membersCannotModifyContent() throws Exception {
        mockMvc.perform(put(CONTENTS_URL + "/1")
                        .contentType("application/json")
                        .content(modifyJson("New Content")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void membersCannotDeleteContent() throws Exception {
        mockMvc.perform(delete(CONTENTS_URL + "/1" ))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "writer", roles = "WRITER")
    void writerCanCreateAndModifyContent() throws Exception {
        // Arrange : content creation
        MvcResult result = mockMvc.perform(post(CONTENTS_URL)
                        .contentType("application/json")
                        .content(createJson("writer", "New Content")))
                        .andExpect(status().isCreated()).andReturn();
        
        // Act : getting the id of the created content and modifying it
        String location = result.getResponse().getHeader("location");
        String createdId = location.substring(location.lastIndexOf("/") + 1);
        
        // Assert
        mockMvc.perform(put(CONTENTS_URL + "/{id}", createdId)
                        .contentType("application/json")
                        .content(modifyJson("Modified"))
                    ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "writer", roles = "WRITER")
    void writerCannotModifyContentOwnedByOthers() throws Exception {
        // Arrange
        Content content = createContent("Temporary", "admin");
        // Act
        Integer id = contentRepository.save(content).getId();
        mockMvc.perform(put(CONTENTS_URL + "/{id}", id)
                        .contentType("application/json")
                        .content(modifyJson("Temporary is modified")))

                        // Assert : the modification fails with HTTP Status Code 403
                        .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "writer", roles = "WRITER")
    void writerCannotDeleteContent() throws Exception {
        mockMvc.perform(delete(CONTENTS_URL + "/1" ))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanCreateContent() throws Exception {
        mockMvc.perform(post(CONTENTS_URL)
                .contentType("application/json")
                .content(createJson("admin", "New Content")))
               .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "writer", roles = "WRITER")
    void writerCanModifyContent() throws Exception {

        // Arrange : create an existing content in the database
        Content content = createContent("Temporary", "writer");
        Integer id = contentRepository.save(content).getId();

        // Act : modify this content via the API
        mockMvc.perform(put(CONTENTS_URL + "/{id}", id)
                        .contentType("application/json")
                        .content(modifyJson("Temporary is modified")))

        // Assert : the modification succeeds with HTTP Status Code 200
                        .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanDeleteContent() throws Exception {
        // Arrange : create an existing content in the database
        Content content = createContent("To Delete", "admin");
        Integer savedId = contentRepository.save(content).getId();

        // Act : delete this content via the API
        mockMvc.perform(delete(CONTENTS_URL + "/" + savedId))

        // Assert : the deletion succeeds without a response body
               .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminGetNotFound() throws Exception {
        mockMvc.perform(get(CONTENTS_URL + "/{id}", Integer.MAX_VALUE)
                .contentType("application/json"))
               .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCannotModifyUnexistingContent() throws Exception {
        mockMvc.perform(put(CONTENTS_URL + "/{id}", Integer.MAX_VALUE % Short.MAX_VALUE)
                        .contentType("application/json")
                        .content(modifyJson("Temporary is modified")))

        // Assert : the modification fails with HTTP Status Code 404
                        .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCannotDeleteUnexistingContent() throws Exception {
        mockMvc.perform(delete(CONTENTS_URL + "/{id}", Short.MAX_VALUE))
                       .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void blankTitleIsNotAccepted() throws Exception {
        mockMvc.perform(post(CONTENTS_URL)
                        .contentType("application/json")
                        .content(createJson("admin", "")))
                       .andExpect(status().isBadRequest());
    }
}