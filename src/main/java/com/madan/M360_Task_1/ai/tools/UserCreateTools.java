package com.madan.M360_Task_1.ai.tools;

import com.madan.M360_Task_1.models.User;
import com.madan.M360_Task_1.service.UserService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserCreateTools {

    @Autowired
    private UserService userService;

    @Tool(description = "Creates a new user immediately. Sets default password 'default123'. Allows assigning initial role (ADMIN/USER).")
    public String createUserNowTool(
            @ToolParam(description = "Username for login") String username,
            @ToolParam(description = "Full name") String name,
            @ToolParam(description = "Email") String email,
            @ToolParam(description = "Contact number") String contactNum,
            @ToolParam(description = "Initial role (ADMIN/USER). Default USER") String roleName) {

        try {
            User saved = userService.createUserWithRole(username, name, email, contactNum, roleName);
            return "✅ User created successfully. ID: " + saved.getId() + " | Role: " + roleName;
        } catch (Exception e) {
            return "Error creating user: " + e.getMessage();
        }
    }
}