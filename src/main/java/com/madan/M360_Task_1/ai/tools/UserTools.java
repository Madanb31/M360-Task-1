package com.madan.M360_Task_1.ai.tools;

import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.service.UserService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class UserTools {

    @Autowired
    private UserService userService;

    @Tool(description = "Looks up a user from the database by their ID. Returns their profile details including name, email, contact, address, and roles.")
    public String userLookupTool(@ToolParam(description = "The UUID of the user to look up") String userId) {

        User user;
        try {
            // Using Service
            user = userService.getUserById(UUID.fromString(userId));
        } catch (Exception e) {
            return "User not found or invalid ID: " + userId;
        }

        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(role -> role.getRoleName()).toList()
                : List.of();

        String address = user.getAddress() != null
                ? user.getAddress().getCity() + ", " + user.getAddress().getState()
                : "No address provided";

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

        // Using Service
        List<User> users = userService.getUsersByName(name);

        if (users.isEmpty()) {
            return "No users found with name: " + name;
        }

        StringBuilder result = new StringBuilder();
        result.append("Found ").append(users.size()).append(" user(s):\n");

        for (User user : users) {
            List<String> roles = user.getRoles() != null
                    ? user.getRoles().stream().map(role -> role.getRoleName()).toList()
                    : List.of();

            result.append(String.format(
                    "- Name: %s, Email: %s, Roles: %s\n",
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

        // Using Service
        List<User> users = userService.getUsersByName(name);

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

    @Tool(description = "Analyzes a user profile by userId using database data only. Returns completeness, missing fields, roles, and a recommendation.")
    public String analyzeUserProfileTool(
            @ToolParam(description = "UUID of the user to analyze") String userId) {

        User user;
        try {
            // Using Service
            user = userService.getUserById(UUID.fromString(userId));
        } catch (Exception e) {
            return "User not found or invalid ID: " + userId;
        }

        boolean hasName = user.getName() != null && !user.getName().isBlank();
        boolean hasEmail = user.getEmail() != null && !user.getEmail().isBlank();
        boolean hasContact = user.getContactNum() != null && !user.getContactNum().isBlank() && !user.getContactNum().equals("0000000000");
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

    @Tool(description = "Lists all users in the database. Returns all users with their IDs, names, emails, roles, and whether they have an address.")
    public String listAllUsersTool() {
        // Using Service
        List<User> users = userService.getAllUsers();
        return formatUserList(users);
    }

    @Tool(description = "Lists ALL users from the database with ID, username, name, email, roles, and whether they have an address.")
    public String listAllUsersDetailedTool() {
        // Using Service
        List<User> users = userService.getAllUsers();
        return formatUserList(users);
    }

    @Tool(description = "Lists users by role (ADMIN/USER). Returns ALL matching users with ID, username, name, email, and roles.")
    public String listUsersByRoleTool(
            @ToolParam(description = "Role name to filter by (e.g., ADMIN, USER)") String roleName) {

        String targetRole = roleName == null ? "" : roleName.trim().toUpperCase();
        if (targetRole.isBlank()) return "Role name is required.";

        // Using Service
        List<User> users = userService.getAllUsers();

        List<User> matched = users.stream()
                .filter(u -> u.getRoles() != null && u.getRoles().stream()
                        .anyMatch(r -> r.getRoleName() != null && r.getRoleName().equalsIgnoreCase(targetRole)))
                .toList();

        if (matched.isEmpty()) return "No users found with role: " + targetRole;

        StringBuilder sb = new StringBuilder();
        sb.append("Users with role ").append(targetRole).append(": ").append(matched.size()).append("\n\n");
        sb.append(formatUserList(matched));
        return sb.toString();
    }

    @Tool(description = "Analyzes ALL user profiles in the database and returns a complete vs incomplete report.")
    public String analyzeAllUsersProfilesTool() {
        // Using Service
        List<User> users = userService.getAllUsers();

        if (users.isEmpty()) return "No users found.";

        int total = users.size();
        int complete = 0;
        int incomplete = 0;
        StringBuilder incompleteDetails = new StringBuilder("❌ Incomplete Profiles:\n");

        for (User user : users) {
            boolean isComplete = checkCompleteness(user);
            if (isComplete) {
                complete++;
            } else {
                incomplete++;
                incompleteDetails.append(String.format("- ID: %s | Name: %s\n", user.getId(), user.getName()));
            }
        }

        if (incomplete == 0) incompleteDetails = new StringBuilder("✅ All profiles complete.\n");

        return """
            📊 All Users Profile Completeness Report
            Total Users: %d
            Complete: %d ✅
            Incomplete: %d ❌
            
            %s
            """.formatted(total, complete, incomplete, incompleteDetails);
    }

    @Tool(description = "Returns statistics about users in the database.")
    public String userStatsTool() {
        // Using Service
        List<User> users = userService.getAllUsers();
        long total = users.size();
        long withAddress = users.stream().filter(u -> u.getAddress() != null).count();

        return String.format("Total Users: %d\nWith Address: %d", total, withAddress);
    }

    // --- Helpers ---

    private String formatUserList(List<User> users) {
        if (users.isEmpty()) return "No users found.";
        StringBuilder sb = new StringBuilder();
        for (User user : users) {
            sb.append(String.format("- ID: %s | Name: %s | Email: %s\n", user.getId(), user.getName(), user.getEmail()));
        }
        return sb.toString();
    }

    private boolean checkCompleteness(User user) {
        return user.getName() != null && !user.getName().isBlank() &&
                user.getEmail() != null && !user.getEmail().isBlank() &&
                user.getAddress() != null;
    }
}