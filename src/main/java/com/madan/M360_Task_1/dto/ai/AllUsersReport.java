package com.madan.M360_Task_1.dto.ai;

import java.util.List;

public record AllUsersReport(
        int totalUsers,
        int completeProfiles,
        int incompleteProfiles,
        List<UserSummary> users,
        String overallRecommendation
) {}