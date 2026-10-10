package com.example.event_ticketing.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * US 12: interim student check. A customer counts as a student when their account
 * email is on a university domain (.edu.au / .edu). Swap this out once the team
 * settles on a stronger verification method; callers only depend on isEligible().
 */
public final class StudentEligibility {

    private static final String[] STUDENT_DOMAINS = {".edu.au", ".edu"};

    private StudentEligibility() {}

    public static boolean isEligible(String email) {
        if (email == null) {
            return false;
        }
        String normalized = email.trim().toLowerCase();
        int at = normalized.lastIndexOf('@');
        if (at < 0) {
            return false;
        }
        String domain = normalized.substring(at + 1);
        for (String suffix : STUDENT_DOMAINS) {
            if (domain.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /** {@code price} reduced by {@code percent}%, rounded to cents. Null-safe on price. */
    public static BigDecimal applyDiscount(BigDecimal price, Integer percent) {
        if (price == null || percent == null || percent <= 0) {
            return price;
        }
        return price.multiply(BigDecimal.valueOf(100 - percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
