package com.madan.M360_Task_1.controllers;

import com.madan.M360_Task_1.dto.AuthResponse;
import com.madan.M360_Task_1.dto.LoginRequest;
import com.madan.M360_Task_1.dto.RegisterRequest;
import com.madan.M360_Task_1.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // POST /auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }

    // POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
