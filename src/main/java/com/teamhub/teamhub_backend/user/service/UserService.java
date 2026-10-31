package com.teamhub.teamhub_backend.user.service;

import com.teamhub.teamhub_backend.common.exception.ResourceNotFoundException;
import com.teamhub.teamhub_backend.user.dto.UserResponse;
import com.teamhub.teamhub_backend.user.entity.User;
import com.teamhub.teamhub_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return UserResponse.fromEntity(getCurrentAuthenticatedUser());
    }

    @Transactional(readOnly = true)
    public User getUserEntityById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}