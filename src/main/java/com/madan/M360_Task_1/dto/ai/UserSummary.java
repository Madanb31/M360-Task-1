package com.madan.M360_Task_1.dto.ai;

import java.util.List;

public record UserSummary(
        String name,
        String email,
        List<String> roles,
        boolean isComplete,
        List<String> missingFields
) {}