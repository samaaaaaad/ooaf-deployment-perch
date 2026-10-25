package com.example.event_ticketing.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for applying for student verification (US 12). */
public class StudentVerificationRequestBody {
    @NotBlank(message = "University is required")
    public String university;
    @NotBlank(message = "Student number must be 4-30 letters, digits or dashes")
    public String studentNumber;
    /** Student card photo as a JPG, PNG or WEBP data URL. */
    @NotBlank(message = "A photo of your student card (JPG, PNG or WEBP) is required")
    public String cardImage;
}
