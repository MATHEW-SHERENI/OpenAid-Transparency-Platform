package com.mathewshereni.open_aid_transparency.ingestion;

import java.util.ArrayList;
import java.util.List;

/**
 * A tiny RFC-4180 CSV reader - the inverse of ReportExportService's writer.
 *
 * It understands quoted fields, so a value containing a comma (e.g. a donor named
 * "Bill, Melinda & co") or an embedded quote ("" -> ") parses back into a single
 * field instead of corrupting the row.
 */
public final class Csv {

    private Csv() {
    }

    /** Parse CSV text into rows of fields. Blank lines are skipped. */
    public static List<List<String>> parse(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        int i = 0;
        int n = content.length();
        while (i < n) {
            char c = content.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // A doubled "" inside a quoted field is a literal quote.
                    if (i + 1 < n && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2;
                    } else {
                        inQuotes = false;
                        i++;
                    }
                } else {
                    field.append(c);
                    i++;
                }
                continue;
            }

            switch (c) {
                case '"' -> {
                    inQuotes = true;
                    i++;
                }
                case ',' -> {
                    current.add(field.toString());
                    field.setLength(0);
                    i++;
                }
                case '\r' -> i++;   // ignore CR; the LF ends the record
                case '\n' -> {
                    current.add(field.toString());
                    field.setLength(0);
                    addRow(rows, current);
                    current = new ArrayList<>();
                    i++;
                }
                default -> {
                    field.append(c);
                    i++;
                }
            }
        }

        // Flush the final field/row if the file doesn't end with a newline.
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            addRow(rows, current);
        }
        return rows;
    }

    /** Append a row unless it's an empty/blank line. */
    private static void addRow(List<List<String>> rows, List<String> row) {
        if (row.size() == 1 && row.get(0).isBlank()) {
            return;
        }
        rows.add(row);
    }
}
