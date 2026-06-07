package com.mathewshereni.open_aid_transparency.funding;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Renders the "funding by recipient" report into downloadable file formats.
 *
 * Kept separate from FundingFlowService so the reporting/aggregation logic stays
 * independent of the presentation format - the same data can be served as JSON,
 * CSV, or PDF.
 */
@Service
public class ReportExportService {

    private static final String[] HEADERS =
            {"recipientId", "countryName", "currency", "totalAmount", "flowCount"};

    // ---------- CSV ----------

    /**
     * Produce RFC-4180 CSV. The escaping matters: some country names contain a
     * comma (e.g. "Congo, Dem. Rep."), which would otherwise split into two
     * columns and corrupt the row.
     */
    public byte[] toCsv(List<FundingByRecipient> rows) {
        StringBuilder sb = new StringBuilder();
        appendCsvRow(sb, HEADERS);
        for (FundingByRecipient r : rows) {
            appendCsvRow(sb, new String[]{
                    String.valueOf(r.recipientId()),
                    r.countryName(),
                    r.currency(),
                    r.totalAmount().toPlainString(),
                    String.valueOf(r.flowCount())
            });
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendCsvRow(StringBuilder sb, String[] fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(csvEscape(fields[i]));
        }
        sb.append("\r\n");   // RFC-4180 line ending
    }

    /** Quote a field only if it must be: contains a comma, quote, or newline. */
    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        boolean mustQuote = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!mustQuote) {
            return value;
        }
        // Inside a quoted field, a literal " is doubled to "".
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    // ---------- PDF ----------

    /** Produce a single-page (or flowing) PDF with a titled table of the report. */
    public byte[] toPdf(List<FundingByRecipient> rows) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Funding by Recipient Country", titleFont);
            title.setSpacingAfter(12f);
            document.add(title);

            PdfPTable table = new PdfPTable(HEADERS.length);
            table.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            for (String header : HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (FundingByRecipient r : rows) {
                table.addCell(new Phrase(String.valueOf(r.recipientId()), cellFont));
                table.addCell(new Phrase(r.countryName(), cellFont));
                table.addCell(new Phrase(r.currency(), cellFont));

                PdfPCell amount = new PdfPCell(new Phrase(r.totalAmount().toPlainString(), cellFont));
                amount.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(amount);

                PdfPCell count = new PdfPCell(new Phrase(String.valueOf(r.flowCount()), cellFont));
                count.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(count);
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF report", e);
        }
        return out.toByteArray();
    }
}
