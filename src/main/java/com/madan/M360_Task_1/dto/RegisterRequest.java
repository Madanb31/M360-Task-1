package com.madan.M360_Task_1.dto;

import jakarta.validation.constraints.*;
import java.util.Set;
import java.util.UUID;

public record RegisterRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 4, message = "Password must be at least 4 characters")
        String password,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email is not valid")
        String email,

        @NotBlank(message = "Contact number is required")
        @Size(min = 10, max = 15, message = "Contact must be 10-15 digits")
        String contactNum,

        // Address fields (optional)
        String street,
        String city,
        String state,
        String zipCode,

        // Role IDs (optional)
        Set<UUID> roleIds
) {}