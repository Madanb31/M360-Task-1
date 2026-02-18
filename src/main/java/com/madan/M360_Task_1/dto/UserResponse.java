package com.madan.M360_Task_1.dto;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String name,
        String email,
        String contactNum,
        String street,
        String city,
        String state,
        String zipCode,
        List<String> roles,
        List<UUID> roleIds
) {}