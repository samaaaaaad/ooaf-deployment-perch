package com.example.event_ticketing.util;

import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * US 12: who counts as a student. A university email (.edu.au / .edu) only lets a customer
 * apply; they become eligible once an admin approves their student verification request
 * (see StudentVerificationService). University-specific events are restricted to them.
 */
public final class StudentEligibility {

    public static final String STUDENTS_ONLY_MESSAGE =
            "This event is for verified university students only. Verify your student status from your account menu to book.";

    private static final String[] STUDENT_DOMAINS = {".edu.au", ".edu"};

    private StudentEligibility() {}

    /** Admin-verified student. The only check that grants student prices or students-only access. */
    public static boolean isEligible(User user) {
        return user != null && user.isStudentVerified();
    }

    /** Whether the email is on a university domain, the precondition for applying. */
    public static boolean hasUniversityEmail(String email) {
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

    /**
     * University-specific events are for verified students only: anyone else is refused
     * when they try to hold a seat or check out.
     */
    public static void requireAllowedToBook(Event event, User customer) {
        if (event.isUniversitySpecific() && !isEligible(customer)) {
            throw new ValidationException(STUDENTS_ONLY_MESSAGE);
        }
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
