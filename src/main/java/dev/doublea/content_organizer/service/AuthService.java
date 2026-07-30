package dev.doublea.content_organizer.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        User testUser = new User("test", passwordEncoder.encode("rawpassword"));
        testUser.setRole(Role.ADMIN);
        userRepository.save(testUser);
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);

        return new LoginResponse(jwt);
    }

    public void register(RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
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

    public void modifyRights(User caller, User member, Role newRole) {
        if ( ! caller.getRole().equals(Role.ADMIN) ) {
            throw new org.springframework.security.access.AccessDeniedException(caller.getUsername() + "does not have enough rights");
        }
        else if ( ! member.getRole().equals(Role.MEMBER)) {
            throw new IllegalArgumentException("Only member can have their rights modified");
        } else if (member.getRole().equals(newRole)) {
            throw new IllegalArgumentException(member.getUsername() + " already has these rights");
        } else {
            member.setRole(newRole);      
        }
    }
    
}
