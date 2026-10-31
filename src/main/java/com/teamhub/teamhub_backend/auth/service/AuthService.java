package com.teamhub.teamhub_backend.auth.service;

import com.teamhub.teamhub_backend.auth.dto.AuthResponse;
import com.teamhub.teamhub_backend.auth.dto.LoginRequest;
import com.teamhub.teamhub_backend.auth.dto.RegisterRequest;
import com.teamhub.teamhub_backend.common.exception.ConflictException;
import com.teamhub.teamhub_backend.user.dto.UserResponse;
import com.teamhub.teamhub_backend.user.entity.User;
import com.teamhub.teamhub_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with email '" + normalizedEmail + "' already exists");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return AuthResponse.of(token, UserResponse.fromEntity(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmail(normalizedEmail).orElseThrow();
        String token = jwtService.generateToken(user);

        return AuthResponse.of(token, UserResponse.fromEntity(user));
    }
}