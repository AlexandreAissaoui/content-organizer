package dev.doublea.content_organizer.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.doublea.content_organizer.dto.auth.LoginRequest;
import dev.doublea.content_organizer.dto.auth.LoginResponse;
import dev.doublea.content_organizer.dto.auth.RegisterRequest;
import dev.doublea.content_organizer.model.Role;
import dev.doublea.content_organizer.model.User;
import dev.doublea.content_organizer.service.AuthService;
import io.jsonwebtoken.lang.Collections;
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

    @DeleteMapping("/delete/{username}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String username, Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("erreur", "L'utilisateur est inconnu"));
        }
        
        Optional<String> present = authService.getUsers().stream()
            .filter(user -> user.equals(username))
            .findFirst();
        // If the user to delete cannot be found -> return 404
        if (present.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("erreur", "L'utilisateur est inconnu"));
        }
        Optional<User> userToDelete = authService.getByUsername(present);
        // If we try to delete an admin -> return 403 forbidden
        if (userToDelete.isPresent() && userToDelete.get().getRole().equals(Role.ADMIN)) {
            return ResponseEntity.status(403).body(Map.of("erreur", "Droits insuffisants"));
        }

        Optional<String> callerName = authService.getUsers().stream()
                .filter(user -> user.equals(authentication.getName()))
                .findFirst();
        Optional<User> caller = authService.getByUsername(callerName);
        // If the caller cannot be found in User DB or he does not have enough rights to suppress members
        if ((! caller.isPresent() ) || ! (caller.get().getRole().equals(Role.ADMIN))) {
            return ResponseEntity.status(403).body(Map.of("erreur", "Vérifiez vos droits d'utilisateur"));
        }
        authService.deleteUser(username);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @GetMapping("/all")
    public List<String> getUsers(Authentication authentication) {
        if (authentication != null) {
            //Optional<String> callerName = authService.getUsers().stream().filter(user -> user.equals(authentication.getName())).findFirst();
            //if (callerName.isPresent())
            return authService.getUsers();
        }
        return Collections.emptyList();
    }
}
