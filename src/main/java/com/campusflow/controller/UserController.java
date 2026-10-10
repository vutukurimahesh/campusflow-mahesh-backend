package com.campusflow.controller;

import com.campusflow.dto.Dtos.*;
import com.campusflow.security.CurrentUser;
import com.campusflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final AuthService auth;

    public UserController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/me")
    public UserResponse me() {
        return auth.me(CurrentUser.id());
    }

    @PutMapping("/me")
    public UserResponse update(@Valid @RequestBody UpdateProfileRequest req) {
        return auth.updateProfile(CurrentUser.id(), req);
    }

    @GetMapping("/search")
    public List<UserResponse> search(@RequestParam(name = "q", defaultValue = "") String q) {
        return auth.search(q);
    }
}
