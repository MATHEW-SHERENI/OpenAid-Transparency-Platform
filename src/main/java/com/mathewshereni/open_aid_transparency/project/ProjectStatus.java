package com.mathewshereni.open_aid_transparency.project;

/**
 * The lifecycle stage of an aid project.
 */
public enum ProjectStatus {
    PLANNED,     // approved/announced but not started
    ACTIVE,      // currently being implemented
    COMPLETED,   // finished
    CANCELLED    // stopped before completion
}
