package com.madan.M360_Task_1.ai.tools;

import com.madan.M360_Task_1.models.Role;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.repository.RoleRepository;
import com.madan.M360_Task_1.repository.UserRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserCreateTools {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserCreateTools(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Tool(description = "Creates a new user immediately. Sets default password 'default123'. Allows assigning initial role (ADMIN/USER).")
    public String createUserNowTool(
            @ToolParam(description = "Username for login") String username,
            @ToolParam(description = "Full name") String name,
            @ToolParam(description = "Email") String email,
            @ToolParam(description = "Contact number") String contactNum,
            @ToolParam(description = "Initial role (ADMIN/USER). Default USER") String roleName) {

        if (userRepository.existsByUsername(username)) return "Error: Username already exists.";
        if (userRepository.existsByEmail(email)) return "Error: Email already exists.";

        String targetRole = (roleName == null || roleName.isBlank()) ? "USER" : roleName.trim().toUpperCase();

        Role role = roleRepository.findByRoleNameIgnoreCase(targetRole)
                .orElseThrow(() -> new RuntimeException("Role not found: " + targetRole));

        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setEmail(email);
        user.setContactNum(contactNum);
        user.setPassword(passwordEncoder.encode("default123"));
        user.setRoles(Set.of(role));

        User saved = userRepository.save(user);

        return "✅ User created successfully. ID: " + saved.getId() + " | Role: " + targetRole;
    }
}