package com.madan.M360_Task_1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email is not valid")
        String email,

        @NotBlank(message = "Contact number is required")
        @Size(min = 10, max = 15, message = "Contact must be 10-15 digits")
        String contactNum,

        // Address fields
        String street,
        String city,
        String state,
        String zipCode,

        // Role IDs
        Set<UUID> roleIds

) {}