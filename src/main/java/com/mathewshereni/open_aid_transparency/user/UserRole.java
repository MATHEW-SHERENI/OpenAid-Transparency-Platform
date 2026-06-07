package com.mathewshereni.open_aid_transparency.user;

/**
 * What a user is allowed to do. Phase 3 (Security) will grant different access
 * to each role - e.g. only ADMIN can import data, anyone CITIZEN can leave feedback.
 */
public enum UserRole {
    CITIZEN,   // general public: browse data, submit feedback
    NGO,       // non-governmental staff
    DONOR,     // donor-organisation staff
    ADMIN      // full administrative access
}
