package com.demo.app.api.controller;

import com.demo.app.api.dto.AuthResponse;
import com.demo.app.api.dto.LoginRequest;
import com.demo.app.api.dto.RegisterRequest;
import com.demo.app.application.service.AuthService;
import com.demo.app.application.service.UserService;
import com.demo.app.domain.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.username(), request.password());
        User user = userService.getByUsername(request.username());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(
                request.username(), request.email(), request.password(), request.displayName());
        String token = authService.login(request.username(), request.password());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }
}
