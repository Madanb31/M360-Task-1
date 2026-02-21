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

