package com.madan.M360_Task_1.ai.tools;

import com.madan.M360_Task_1.models.Role;
import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.repository.RoleRepository;
import com.madan.M360_Task_1.repository.UserRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class UserManagementTools {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. CREATE USER
    @Tool(description = "Creates a new user with the given details. Assigns default password 'default123' and 'USER' role initially. Returns the new User ID.")
    public String createUserTool(
            @ToolParam(description = "Username for login") String username,
            @ToolParam(description = "Full Name") String name,
            @ToolParam(description = "Email address") String email,
            @ToolParam(description = "Contact number") String contactNum,
            @ToolParam(description = "Role to assign (ADMIN or USER). Default is USER.") String roleName) {

        if (userRepository.existsByUsername(username)) return "Error: Username already exists.";
        if (userRepository.existsByEmail(email)) return "Error: Email already exists.";

        try {
            User user = new User();
            user.setUsername(username);
            user.setName(name);
            user.setEmail(email);
            user.setContactNum(contactNum);
            user.setPassword(passwordEncoder.encode("default123")); // Default password

            // Handle Role
            String targetRole = (roleName != null && !roleName.isBlank()) ? roleName : "USER";
            Role userRole = roleRepository.findByRoleNameIgnoreCase(targetRole)
                    .orElseThrow(() -> new RuntimeException("Role '" + targetRole + "' not found"));

            user.setRoles(Set.of(userRole));

            User savedUser = userRepository.save(user);
            return "User created successfully. ID: " + savedUser.getId();
        } catch (Exception e) {
            return "Error creating user: " + e.getMessage();
        }
    }

    // 2. DELETE USER
    @Tool(description = "Deletes a user permanently from the database using their ID.")
    public String deleteUserTool(@ToolParam(description = "UUID of the user to delete") String userId) {
        try {
            UUID id = UUID.fromString(userId);
            if (!userRepository.existsById(id)) return "Error: User not found.";

            userRepository.deleteById(id);
            return "User deleted successfully.";
        } catch (Exception e) {
            return "Error deleting user: " + e.getMessage();
        }
    }

    // 3. ASSIGN ROLE
    @Tool(description = "Assigns a specific role (e.g., ADMIN, USER) to a user.")
    @Transactional // Needed for modifying collections
    public String assignRoleTool(
            @ToolParam(description = "UUID of the user") String userId,
            @ToolParam(description = "Role name to assign (e.g. ADMIN)") String roleName) {

        try {
            User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
            if (user == null) return "Error: User not found.";

            Role role = roleRepository.findByRoleNameIgnoreCase(roleName).orElse(null);
            if (role == null) return "Error: Role '" + roleName + "' not found.";

            user.getRoles().add(role); // Add role
            userRepository.save(user);

            return "Role " + roleName + " assigned to user " + user.getUsername();
        } catch (Exception e) {
            return "Error assigning role: " + e.getMessage();
        }
    }

    // 4. REMOVE ROLE
    @Tool(description = "Removes a specific role from a user.")
    @Transactional
    public String removeRoleTool(
            @ToolParam(description = "UUID of the user") String userId,
            @ToolParam(description = "Role name to remove") String roleName) {

        try {
            User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
            if (user == null) return "Error: User not found.";

            Role role = roleRepository.findByRoleNameIgnoreCase(roleName).orElse(null);
            if (role == null) return "Error: Role '" + roleName + "' not found.";

            if (!user.getRoles().contains(role)) {
                return "User does not have role " + roleName;
            }

            user.getRoles().remove(role);
            userRepository.save(user);

            return "Role " + roleName + " removed from user " + user.getUsername();
        } catch (Exception e) {
            return "Error removing role: " + e.getMessage();
        }
    }
}
