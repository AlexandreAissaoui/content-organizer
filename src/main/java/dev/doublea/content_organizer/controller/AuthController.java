package dev.doublea.content_organizer.controller;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.doublea.content_organizer.dto.auth.LoginRequest;
import dev.doublea.content_organizer.dto.auth.LoginResponse;
import dev.doublea.content_organizer.dto.auth.RegisterRequest;
import dev.doublea.content_organizer.model.Role;
import dev.doublea.content_organizer.model.User;
import dev.doublea.content_organizer.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.created(URI.create("http://localhost:8080/api/users/" + request.username())).build();
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String username, Authentication authentication) {
        String unknown = "Unknown user";
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", unknown));
        }
        // If the user to delete cannot be found -> return 404
        String present = authService.getUsers().stream()
            .filter(user -> user.equals(username))
            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, unknown));
        User userToDelete = authService.getByUsername(present).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, unknown));
        // If we try to delete an admin -> return 403 forbidden
        if (userToDelete.getRole().equals(Role.ADMIN)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not enough rights"));
        }

        String callerName = authService.getUsers().stream()
                .filter(user -> user.equals(authentication.getName()))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, unknown));
        User caller = authService.getByUsername(callerName).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, unknown));
        // If the caller does not have enough rights to suppress members
        if (! caller.getRole().equals(Role.ADMIN))  {
            return ResponseEntity.status(403).body(Map.of("error", "Check your rights"));
        }
        authService.deleteUser(username);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @GetMapping("/users")
    public List<String> getUsers(Authentication authentication) {
        if ( ( authentication != null) && (authService.getByUsername(authentication.getName()).isPresent()) ) {
            return authService.getUsers();
        }
        return Collections.emptyList();
    }

    @PutMapping("/users/{username}")
    public ResponseEntity<Map<String, String>> modifyRights(@PathVariable String username, @RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unknown user"));
        }
        return authService.modifyRights(authentication, username, request);
    }
}