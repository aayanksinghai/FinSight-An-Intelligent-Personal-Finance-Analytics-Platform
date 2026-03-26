package com.finsight.user.api.profile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateUserProfileRequest(
        @Size(max = 120) String fullName,
        @Size(max = 120) String city,
        @Size(max = 40) String ageGroup,
        @DecimalMin(value = "0.00", inclusive = false) BigDecimal monthlyIncome) {
}

