package dev.doublea.content_organizer.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
                       UserRepository userRepository, PasswordEncoder passwordEncoder) {
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

    public void register(RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password())
        );
        userRepository.save(user);
    }

    public List<String> getUsers() {
        return userRepository.findAll().stream().map(User::getUsername).toList();
    }

    public Optional<User> getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getByUsername(Optional<String> username) {
        if (username.isPresent())
            return userRepository.findByUsername(username.get());
        return Optional.empty();
    }

    @Transactional
    public void deleteUser(String username) {
        userRepository.deleteByUsername(username);
    }

    public ResponseEntity<Map<String, String>> modifyRights(Authentication authentication, String memberName, Map<String, String> request) {
        String error = "error";
        if (request.get("role") == null) {
            return ResponseEntity.status(400).body(Map.of(error, "Role is required"));
        }
        String roleString = request.get("role").toUpperCase();
        if ((! roleString.equals("ADMIN")) && (! roleString.equals("WRITER")) && (! roleString.equals("MEMBER")) ) {
            return ResponseEntity.status(400).body(Map.of(error, "Unknown role : " +roleString));
        }
        Role newRole = Role.valueOf(roleString);
        Optional<User> memberOptional = userRepository.findByUsername(memberName);
        Optional<User> callerOptional = userRepository.findByUsername(authentication.getName());
        User member = memberOptional.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown user"));
        User caller = callerOptional.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown user"));
        if ( ! caller.getRole().equals(Role.ADMIN) ) {
            return ResponseEntity.status(403).body(Map.of(error, "Only administrators can modify rights"));
        }
        else if ( member.getRole().equals(Role.ADMIN)) {
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
