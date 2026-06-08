package com.mathewshereni.open_aid_transparency.funding;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ReportExportService. The CSV escaping is the part with real
 * correctness risk - especially country names that contain a comma.
 */
class ReportExportServiceTest {

    private final ReportExportService service = new ReportExportService();

    private String csv(List<FundingByRecipient> rows) {
        return new String(service.toCsv(rows), StandardCharsets.UTF_8);
    }

    @Test
    void csv_writesHeaderAndPlainRowWithCrlf() {
        String out = csv(List.of(
                new FundingByRecipient(1L, "KEN", "Kenya", "USD", new BigDecimal("12834880127.46"), 6L)));

        assertThat(out).startsWith("recipientId,isoCode,countryName,currency,totalAmount,flowCount\r\n");
        // A name with no special characters is NOT quoted.
        assertThat(out).contains("1,KEN,Kenya,USD,12834880127.46,6\r\n");
    }

    @Test
    void csv_quotesFieldsContainingACommaSoTheRowIsNotCorrupted() {
        String out = csv(List.of(
                new FundingByRecipient(11L, "COD", "Congo, Dem. Rep.", "USD", new BigDecimal("100.00"), 4L)));

        // The comma inside the name must be wrapped in quotes, keeping the columns aligned.
        assertThat(out).contains("11,COD,\"Congo, Dem. Rep.\",USD,100.00,4\r\n");
    }

    @Test
    void csv_escapesEmbeddedQuotesByDoublingThem() {
        String out = csv(List.of(
                new FundingByRecipient(2L, "TBF", "The \"Best\" Fund", "USD", new BigDecimal("1.00"), 1L)));

        // A literal " becomes "" and the whole field is quoted.
        assertThat(out).contains("\"The \"\"Best\"\" Fund\"");
    }

    @Test
    void csv_amountIsPlainNotScientificNotation() {
        // A large value must not come out as "1.23E10". (Nigeria/NGA avoids a stray
        // uppercase 'E' from other fields so the doesNotContain check stays meaningful.)
        String out = csv(List.of(
                new FundingByRecipient(2L, "NGA", "Nigeria", "USD", new BigDecimal("12300000000"), 1L)));

        assertThat(out).contains("12300000000");
        assertThat(out).doesNotContain("E");
    }

    @Test
    void pdf_producesANonEmptyPdfDocument() {
        byte[] pdf = service.toPdf(List.of(
                new FundingByRecipient(1L, "KEN", "Kenya", "USD", new BigDecimal("100.00"), 2L)));

        assertThat(pdf).isNotEmpty();
        // Every PDF file begins with the "%PDF" magic marker.
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
