package com.madan.M360_Task_1.dto.ai;

import java.util.List;

public record UserInfo(
        String name,
        String email,
        String contactNum,
        String city,
        String state,
        List<String> roles,
        boolean hasAddress
) {}