package dev.doublea.content_organizer.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import dev.doublea.content_organizer.dto.auth.LoginRequest;
import dev.doublea.content_organizer.dto.auth.LoginResponse;
import dev.doublea.content_organizer.dto.auth.ModifyRequest;
import dev.doublea.content_organizer.dto.auth.RegisterRequest;
import dev.doublea.content_organizer.model.Role;
import dev.doublea.content_organizer.model.User;
import dev.doublea.content_organizer.repository.UserRepository;
import dev.doublea.content_organizer.security.JwtUtils;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        }
        catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);

        return new LoginResponse(jwt);
    }

    /**
     * Creates a new user account with the default {@code MEMBER} role.
     *
     * <p>The previous implementation checked {@code findByUsername()} before
     * saving — a concurrent request could pass the same check and both inserts
     * would race, producing either a duplicate row or an unhandled exception.</p>
     *
     * <p>Relying on the database's own {@code UNIQUE} constraint and catching
     * {@link DataIntegrityViolationException} is the only atomic path: the
     * INSERT either succeeds or fails, and the failure is deterministic.</p>
     */
    public void register(RegisterRequest request) {
        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password())
        );

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'WRITER', 'ADMIN')")
    public List<String> getUsers() {
        return userRepository.findAll().stream().map(User::getUsername).toList();
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'WRITER', 'ADMIN')")
    public Optional<User> getByUsername(String username) {
        return userRepository.findByUsername(username);
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'WRITER', 'ADMIN')")
    public Optional<User> getByUsername(Optional<String> username) {
        if (username.isPresent())
            return userRepository.findByUsername(username.get());
        return Optional.empty();
    }


    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteUser(String username) {
        userRepository.deleteByUsername(username);
        return ResponseEntity.ok(Map.of("success", "User deleted successfully"));
    }


    /**
     * Changes a user's role. The role is validated by the {@code @NotNull}
     * constraint on {@link ModifyRequest} — no manual check is needed.
     *
     * <p>{@code @Transactional} makes the loaded {@code member} entity managed:
     * calling {@code setRole()} marks it dirty, and Hibernate flushes the
     * UPDATE automatically at commit. No explicit {@code save()} or JPQL is
     * required.</p>
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> modifyRights(Authentication authentication, String memberName, ModifyRequest request) {
        String error = "error";

        Role newRole = request.role();

        // Check if member exists and has adequate rights
        User member = userRepository.findByUsername(memberName)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown user"));
        if ( member.getRole().equals(Role.ADMIN)) {
            return ResponseEntity.status(403).body(Map.of(error, "Administrators cannot have their rights modified"));
        } 
        else if (member.getRole().equals(newRole)) {
            return ResponseEntity.status(400).body(Map.of(error, "Member already has the role: " +newRole));
        
        } else {
            member.setRole(newRole);      
        }
        return ResponseEntity.ok(Map.of("success", "Member has new role : " +newRole));
    }
}
