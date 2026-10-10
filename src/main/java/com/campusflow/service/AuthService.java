package com.campusflow.service;

import com.campusflow.dto.Dtos.*;
import com.campusflow.entity.User;
import com.campusflow.exception.ApiException;
import com.campusflow.repository.UserRepository;
import com.campusflow.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u = users.save(u);
        return new AuthResponse(jwt.generate(u.getId(), u.getEmail()), UserResponse.from(u));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User u = users.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new BadCredentialsException("bad credentials");
        }
        return new AuthResponse(jwt.generate(u.getId(), u.getEmail()), UserResponse.from(u));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return UserResponse.from(users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found")));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        u.setName(req.name().trim());
        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            if (req.currentPassword() == null || !encoder.matches(req.currentPassword(), u.getPasswordHash())) {
                throw ApiException.badRequest("Current password is incorrect");
            }
            u.setPasswordHash(encoder.encode(req.newPassword()));
        }
        return UserResponse.from(users.save(u));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> search(String q) {
        if (q == null || q.trim().length() < 2) {
            return List.of();
        }
        return users.search(q.trim(), org.springframework.data.domain.PageRequest.of(0, 8))
                .stream().map(UserResponse::from).toList();
    }
}
