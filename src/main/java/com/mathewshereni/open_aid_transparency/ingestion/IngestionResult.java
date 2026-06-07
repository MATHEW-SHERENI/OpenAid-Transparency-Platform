package com.mathewshereni.open_aid_transparency.ingestion;

/**
 * A small summary returned after an import run, so the caller can see what happened.
 */
public record IngestionResult(
        int fetched,
        int created,
        int skipped
) {
}
