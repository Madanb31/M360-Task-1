package com.madan.M360_Task_1.service;

import com.madan.M360_Task_1.dto.AuthResponse;
import com.madan.M360_Task_1.dto.LoginRequest;
import com.madan.M360_Task_1.dto.RegisterRequest;
import com.madan.M360_Task_1.models.AuthUser;
import com.madan.M360_Task_1.repository.AuthUserRepository;
import com.madan.M360_Task_1.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // REGISTER
    public String register(RegisterRequest request) {

        // Check if username already exists
        if (authUserRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username already exists"
            );
        }

        // Validate role
        String role = request.role().toUpperCase();
        if (!role.equals("ADMIN") && !role.equals("USER")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role must be ADMIN or USER"
            );
        }

        // Create auth user
        AuthUser authUser = new AuthUser();
        authUser.setUsername(request.username());
        authUser.setPassword(passwordEncoder.encode(request.password()));  // BCrypt encrypt!
        authUser.setRole(role);

        authUserRepository.save(authUser);

        return "User registered successfully";
    }

    // LOGIN
    public AuthResponse login(LoginRequest request) {

        // Find user by username
        AuthUser authUser = authUserRepository.findByUsername(request.username())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid username or password"
                        )
                );

        // Check password
        if (!passwordEncoder.matches(request.password(), authUser.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(authUser.getUsername(), authUser.getRole());

        // Return token + user info
        return new AuthResponse(token, authUser.getUsername(), authUser.getRole());
    }
}
