package com.finsight.user.api.profile;

import java.math.BigDecimal;

public record UserProfileResponse(
        String email,
        String fullName,
        String city,
        String ageGroup,
        BigDecimal monthlyIncome) {
}

