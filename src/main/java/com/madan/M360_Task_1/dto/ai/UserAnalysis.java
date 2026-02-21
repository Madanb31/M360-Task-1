package com.madan.M360_Task_1.dto.ai;

import java.util.List;

public record UserAnalysis(
        String name,
        String email,
        String contactNum,
        String city,
        String state,
        List<String> roles,
        boolean hasAddress,
        boolean isProfileComplete,
        List<String> missingFields,
        String recommendation
) {}
