package com.mathewshereni.open_aid_transparency.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the CSV parser. The quoting rules are the point: a comma or quote
 * inside a field must NOT break the row into the wrong number of columns.
 */
class CsvTest {

    @Test
    void parsesPlainRows() {
        List<List<String>> rows = Csv.parse("a,b,c\n1,2,3\n");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsExactly("a", "b", "c");
        assertThat(rows.get(1)).containsExactly("1", "2", "3");
    }

    @Test
    void keepsCommaInsideQuotedFieldAsOneColumn() {
        List<List<String>> rows = Csv.parse("name,iso\n\"Congo, Dem. Rep.\",COD\n");

        assertThat(rows.get(1)).containsExactly("Congo, Dem. Rep.", "COD");
    }

    @Test
    void unescapesDoubledQuotes() {
        List<List<String>> rows = Csv.parse("x\n\"The \"\"Best\"\" Fund\"\n");

        assertThat(rows.get(1)).containsExactly("The \"Best\" Fund");
    }

    @Test
    void handlesCrlfLineEndings() {
        List<List<String>> rows = Csv.parse("a,b\r\n1,2\r\n");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(1)).containsExactly("1", "2");
    }

    @Test
    void skipsBlankLines() {
        List<List<String>> rows = Csv.parse("a,b\n\n1,2\n\n");

        assertThat(rows).hasSize(2);
    }

    @Test
    void parsesLastRowWithoutTrailingNewline() {
        List<List<String>> rows = Csv.parse("a,b\n1,2");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(1)).containsExactly("1", "2");
    }
}
