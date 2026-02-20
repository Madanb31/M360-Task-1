package com.madan.M360_Task_1.service;

import com.madan.M360_Task_1.dto.AuthResponse;
import com.madan.M360_Task_1.dto.LoginRequest;
import com.madan.M360_Task_1.dto.RegisterRequest;
import com.madan.M360_Task_1.models.Address;
import com.madan.M360_Task_1.models.Role;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.repository.RoleRepository;
import com.madan.M360_Task_1.repository.UserRepository;
import com.madan.M360_Task_1.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // REGISTER
    public String register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username already exists"
            );
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email already exists"
            );
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setEmail(request.email());
        user.setContactNum(request.contactNum());

        // Address
        if (request.street() != null || request.city() != null
                || request.state() != null || request.zipCode() != null) {
            Address address = new Address();
            address.setStreet(request.street());
            address.setCity(request.city());
            address.setState(request.state());
            address.setZipCode(request.zipCode());
            user.setAddress(address);
        }

        // Always USER role
        Role userRole = roleRepository.findByRoleNameIgnoreCase("USER")
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Default USER role not configured"
                        )
                );
        user.setRoles(Set.of(userRole));

        userRepository.save(user);
        return "User registered successfully";
    }

    // LOGIN
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid username or password"
                        )
                );

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }

        // Pick the HIGHEST role (ADMIN > USER)
        String role = user.getRoles().stream()
                .map(Role::getRoleName)
                .filter(r -> r.equals("ADMIN"))
                .findFirst()
                .orElse("USER");

        String token = jwtUtil.generateToken(user.getUsername(), role);

        return new AuthResponse(token, user.getUsername(), role);
    }
}