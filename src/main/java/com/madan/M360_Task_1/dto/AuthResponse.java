package com.madan.M360_Task_1.dto;

public record AuthResponse(
        String token,
        String username,
        String role
) {
}
