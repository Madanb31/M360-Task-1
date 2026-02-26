package com.madan.M360_Task_1.ai.tools;

import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.repository.UserRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class UserTools {

    @Autowired
    private UserRepository userRepository;

    @Tool(description = "Looks up a user from the database by their ID. Returns their profile details including name, email, contact, address, and roles.")
    public String userLookupTool(@ToolParam(description = "The UUID of the user to look up") String userId) {

        User user;
        try {
            user = userRepository.findById(UUID.fromString(userId)).orElse(null);
        } catch (Exception e) {
            return "Invalid user ID format: " + userId;
        }

        if (user == null) {
            return "User not found with ID: " + userId;
        }

        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream()
                .map(role -> role.getRoleName())
                .toList()
                : List.of();

        String address = user.getAddress() != null
                ? user.getAddress().getCity() + ", " + user.getAddress().getState()
                : "No address";

        return String.format(
                "Name: %s, Email: %s, Contact: %s, Address: %s, Roles: %s",
                user.getName(),
                user.getEmail(),
                user.getContactNum(),
                address,
                roleNames
        );
    }

    @Tool(description = "Searches for users by name in the database. Returns matching users with their names and emails. Use this when asked to find users by name.")
    public String userSearchTool(@ToolParam(description = "The name to search for") String name) {

        List<User> users = userRepository.findAllByNameContainingIgnoreCase(name);

        if (users.isEmpty()) {
            return "No users found with name: " + name;
        }

        StringBuilder result = new StringBuilder();
        result.append("Found ").append(users.size()).append(" user(s):\n");

        for (User user : users) {
            List<String> roles = user.getRoles() != null
                    ? user.getRoles().stream()
                    .map(role -> role.getRoleName())
                    .toList()
                    : List.of();

            result.append(String.format(
                    "- Name: %s, Email: %s, Roles: %s\n",
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    roles
            ));
        }

        return result.toString();
    }

    @Tool(description = "Finds users by a partial name match (case-insensitive). Returns ALL matches with ID, name, email, and roles. Use this tool whenever user asks to 'find user' or 'search user'.")
    public String findUsersByNameTool(
            @ToolParam(description = "Name or partial name to search for") String name) {

        List<User> users = userRepository.findAllByNameContainingIgnoreCase(name);

        if (users.isEmpty()) {
            return "No users found matching name: " + name;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(users.size()).append(" user(s) matching '").append(name).append("':\n");

        for (User user : users) {
            List<String> roles = (user.getRoles() != null)
                    ? user.getRoles().stream().map(r -> r.getRoleName()).toList()
                    : List.of();

            sb.append(String.format(
                    "- ID: %s | Name: %s | Email: %s | Roles: %s\n",
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    roles
            ));
        }

        return sb.toString();
    }

    @Tool(description = "Analyzes a user profile by userId using database data only. Returns completeness, missing fields, roles, and a recommendation. Use this for requests like 'analyze user', 'analyze him', 'check profile completeness'.")
    public String analyzeUserProfileTool(
            @ToolParam(description = "UUID of the user to analyze") String userId) {

        User user;
        try {
            user = userRepository.findById(UUID.fromString(userId)).orElse(null);
        } catch (Exception e) {
            return "Invalid userId format: " + userId;
        }

        if (user == null) {
            return "User not found with ID: " + userId;
        }

        boolean hasName = user.getName() != null && !user.getName().isBlank();
        boolean hasEmail = user.getEmail() != null && !user.getEmail().isBlank();
        boolean hasContact = user.getContactNum() != null && !user.getContactNum().isBlank()
                && !user.getContactNum().equals("0000000000");
        boolean hasRoles = user.getRoles() != null && !user.getRoles().isEmpty();
        boolean hasAddress = user.getAddress() != null;

        List<String> missing = new java.util.ArrayList<>();
        if (!hasName) missing.add("name");
        if (!hasEmail) missing.add("email");
        if (!hasContact) missing.add("contactNum");
        if (!hasRoles) missing.add("roles");
        if (!hasAddress) missing.add("address");

        List<String> roleNames = hasRoles
                ? user.getRoles().stream().map(r -> r.getRoleName()).toList()
                : List.of();

        boolean complete = missing.isEmpty();

        String recommendation = complete
                ? "Profile is complete. No action needed."
                : "Profile is incomplete. Please update missing fields: " + missing;

        String addressText = hasAddress
                ? user.getAddress().getStreet() + ", " + user.getAddress().getCity() + ", " + user.getAddress().getState() + " - " + user.getAddress().getZipCode()
                : "No address";

        return """
            📋 User Profile Analysis
            - ID: %s
            - Name: %s
            - Email: %s
            - Contact: %s
            - Address: %s
            - Roles: %s
            
            Profile Status: %s
            Missing Fields: %s
            Recommendation: %s
            """.formatted(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getContactNum(),
                addressText,
                roleNames,
                complete ? "Complete ✅" : "Incomplete ❌",
                missing,
                recommendation
        );
    }

    // NEW TOOL: List all users
    @Tool(description = "Lists all users in the database. Returns all users with their IDs, names, emails, roles, and whether they have an address.")
    public String listAllUsersTool() {

        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            return "No users in the database.";
        }

        StringBuilder result = new StringBuilder();
        result.append("Total users: ").append(users.size()).append("\n\n");

        for (User user : users) {
            List<String> roles = user.getRoles() != null
                    ? user.getRoles().stream()
                    .map(role -> role.getRoleName())
                    .toList()
                    : List.of();

            result.append(String.format(
                    "- ID: %s, Name: %s, Email: %s, Roles: %s, Has Address: %s\n",
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    roles,
                    user.getAddress() != null ? "Yes" : "No"
            ));
        }

        return result.toString();
    }

    @Tool(description = "Lists ALL users from the database with ID, username, name, email, roles, and whether they have an address. Use this for requests like 'list all users'.")
    public String listAllUsersDetailedTool() {

        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            return "No users found in the system.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Total users: ").append(users.size()).append("\n\n");

        for (User user : users) {
            List<String> roles = (user.getRoles() != null)
                    ? user.getRoles().stream().map(r -> r.getRoleName()).toList()
                    : List.of();

            sb.append(String.format(
                    "- ID: %s | Username: %s | Name: %s | Email: %s | Roles: %s | HasAddress: %s\n",
                    user.getId(),
                    user.getUsername(),
                    user.getName(),
                    user.getEmail(),
                    roles,
                    user.getAddress() != null ? "Yes" : "No"
            ));
        }

        return sb.toString();
    }

    @Tool(description = "Lists users by role (ADMIN/USER). Returns ALL matching users with ID, username, name, email, and roles. Use this for requests like 'list all admins'.")
    public String listUsersByRoleTool(
            @ToolParam(description = "Role name to filter by (e.g., ADMIN, USER)") String roleName) {

        String targetRole = roleName == null ? "" : roleName.trim().toUpperCase();
        if (targetRole.isBlank()) {
            return "Role name is required (e.g., ADMIN or USER).";
        }

        List<User> users = userRepository.findAll();

        List<User> matched = users.stream()
                .filter(u -> u.getRoles() != null && u.getRoles().stream()
                        .anyMatch(r -> r.getRoleName() != null && r.getRoleName().equalsIgnoreCase(targetRole)))
                .toList();

        if (matched.isEmpty()) {
            return "No users found with role: " + targetRole;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Users with role ").append(targetRole).append(": ").append(matched.size()).append("\n\n");

        for (User user : matched) {
            List<String> roles = (user.getRoles() != null)
                    ? user.getRoles().stream().map(r -> r.getRoleName()).toList()
                    : List.of();

            sb.append(String.format(
                    "- ID: %s | Username: %s | Name: %s | Email: %s | Roles: %s\n",
                    user.getId(),
                    user.getUsername(),
                    user.getName(),
                    user.getEmail(),
                    roles
            ));
        }

        return sb.toString();
    }

    @Tool(description = "Analyzes ALL user profiles in the database and returns a complete vs incomplete report with missing fields and recommendations. Use this when asked to analyze all users or run profile completeness checks.")
    public String analyzeAllUsersProfilesTool() {

        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            return "No users found in the system.";
        }

        int total = users.size();
        int complete = 0;
        int incomplete = 0;

        StringBuilder incompleteDetails = new StringBuilder();
        incompleteDetails.append("❌ Incomplete Profiles:\n");

        for (User user : users) {
            boolean hasName = user.getName() != null && !user.getName().isBlank();
            boolean hasEmail = user.getEmail() != null && !user.getEmail().isBlank();
            boolean hasContact = user.getContactNum() != null && !user.getContactNum().isBlank()
                    && !user.getContactNum().equals("0000000000");
            boolean hasRoles = user.getRoles() != null && !user.getRoles().isEmpty();
            boolean hasAddress = user.getAddress() != null;

            List<String> missing = new java.util.ArrayList<>();
            if (!hasName) missing.add("name");
            if (!hasEmail) missing.add("email");
            if (!hasContact) missing.add("contactNum");
            if (!hasRoles) missing.add("roles");
            if (!hasAddress) missing.add("address");

            if (missing.isEmpty()) {
                complete++;
            } else {
                incomplete++;

                List<String> roleNames = hasRoles
                        ? user.getRoles().stream().map(r -> r.getRoleName()).toList()
                        : List.of();

                incompleteDetails.append(String.format(
                        "- ID: %s | Name: %s | Email: %s | Roles: %s | Missing: %s\n",
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        roleNames,
                        missing
                ));
            }
        }

        if (incomplete == 0) {
            incompleteDetails = new StringBuilder("✅ All user profiles are complete.\n");
        }

        String recommendation = (incomplete == 0)
                ? "No action required."
                : "Ask users with incomplete profiles to update missing fields (especially address/contact).";

        return """
            📊 All Users Profile Completeness Report
            
            Total Users: %d
            Complete Profiles: %d ✅
            Incomplete Profiles: %d ❌
            
            %s
            
            Recommendation: %s
            """.formatted(total, complete, incomplete, incompleteDetails, recommendation);
    }

    // NEW TOOL: Get user statistics
    @Tool(description = "Returns statistics about users in the database including total count, users with/without address, and role distribution.")
    public String userStatsTool() {

        List<User> users = userRepository.findAll();

        long totalUsers = users.size();
        long withAddress = users.stream()
                .filter(u -> u.getAddress() != null)
                .count();
        long withoutAddress = totalUsers - withAddress;

        long adminCount = users.stream()
                .filter(u -> u.getRoles() != null && u.getRoles().stream()
                        .anyMatch(r -> r.getRoleName().equals("ADMIN")))
                .count();
        long userCount = totalUsers - adminCount;

        return String.format(
                "User Statistics:\n" +
                        "Total Users: %d\n" +
                        "With Address: %d\n" +
                        "Without Address: %d\n" +
                        "Admin Users: %d\n" +
                        "Regular Users: %d",
                totalUsers, withAddress, withoutAddress, adminCount, userCount
        );
    }

}

