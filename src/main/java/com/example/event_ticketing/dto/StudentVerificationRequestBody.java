package com.example.event_ticketing.dto;

/** Body for applying for student verification (US 12). */
public class StudentVerificationRequestBody {
    public String university;
    public String studentNumber;
    /** Student card photo as a JPG, PNG or WEBP data URL. */
    public String cardImage;
}
