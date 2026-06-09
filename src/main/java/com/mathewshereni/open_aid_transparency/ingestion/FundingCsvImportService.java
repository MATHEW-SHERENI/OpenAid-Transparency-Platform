package com.mathewshereni.open_aid_transparency.ingestion;

import com.mathewshereni.open_aid_transparency.donor.Donor;
import com.mathewshereni.open_aid_transparency.donor.DonorRepository;
import com.mathewshereni.open_aid_transparency.funding.FundingFlow;
import com.mathewshereni.open_aid_transparency.funding.FundingFlowRepository;
import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import com.mathewshereni.open_aid_transparency.recipient.RecipientRepository;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoal;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Imports funding flows from an admin-uploaded CSV.
 *
 * Expected columns (header row required, order-independent):
 *   donor, recipientIso, year, amount, currency   (sdgGoal optional, 1..17)
 *
 * Both the donor (by name) and recipient (by ISO code) MUST already exist - rows
 * referencing unknown ones are skipped, mirroring how the World Bank import skips
 * untracked countries. A row already present is skipped too, so re-uploading the
 * same file is idempotent.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FundingCsvImportService {

    private static final List<String> REQUIRED_COLUMNS =
            List.of("donor", "recipientiso", "year", "amount", "currency");

    private final DonorRepository donorRepository;
    private final RecipientRepository recipientRepository;
    private final FundingFlowRepository fundingFlowRepository;
    private final SdgGoalRepository sdgGoalRepository;

    @Transactional
    public IngestionResult importCsv(String csvContent) {
        List<List<String>> rows = Csv.parse(csvContent);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV is empty.");
        }

        Map<String, Integer> columns = headerIndex(rows.get(0));

        int fetched = 0;
        int created = 0;
        int skipped = 0;
        for (int i = 1; i < rows.size(); i++) {
            fetched++;
            if (importRow(rows.get(i), columns)) {
                created++;
            } else {
                skipped++;
            }
        }

        IngestionResult result = new IngestionResult(fetched, created, skipped);
        log.info("Funding CSV import: {}", result);
        return result;
    }

    /** @return true if a flow was created, false if the row was skipped. */
    private boolean importRow(List<String> row, Map<String, Integer> columns) {
        try {
            String donorName = value(row, columns, "donor");
            String iso = value(row, columns, "recipientiso");
            String currency = value(row, columns, "currency");
            int year = Integer.parseInt(value(row, columns, "year"));
            BigDecimal amount = new BigDecimal(value(row, columns, "amount")).setScale(2, RoundingMode.HALF_UP);

            if (donorName.isBlank() || iso.isBlank() || currency.isBlank()) {
                return false;
            }

            Donor donor = donorRepository.findByNameIgnoreCase(donorName).orElse(null);
            Recipient recipient = recipientRepository.findByIsoCodeIgnoreCase(iso).orElse(null);
            if (donor == null || recipient == null) {
                return false;   // reference data must exist first
            }
            if (fundingFlowRepository.existsByRecipientAndYearAndDonor(recipient, year, donor)) {
                return false;   // already imported
            }

            fundingFlowRepository.save(FundingFlow.builder()
                    .donor(donor)
                    .recipient(recipient)
                    .currency(currency.toUpperCase(Locale.ROOT))
                    .year(year)
                    .amount(amount)
                    .sdgGoal(resolveSdgGoal(row, columns))   // optional category tag
                    .build());
            return true;
        } catch (RuntimeException malformed) {
            // A single bad row (e.g. non-numeric amount) is skipped, not fatal.
            return false;
        }
    }

    /** Map lower-cased header names to their column index, validating the required set. */
    private Map<String, Integer> headerIndex(List<String> header) {
        Map<String, Integer> index = IntStream.range(0, header.size())
                .boxed()
                .collect(Collectors.toMap(i -> header.get(i).trim().toLowerCase(Locale.ROOT), i -> i, (a, b) -> a));
        List<String> missing = REQUIRED_COLUMNS.stream().filter(c -> !index.containsKey(c)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("CSV is missing required column(s): " + missing);
        }
        return index;
    }

    private String value(List<String> row, Map<String, Integer> columns, String column) {
        int idx = columns.get(column);
        return idx < row.size() ? row.get(idx).trim() : "";
    }

    /** Optional "sdgGoal" column (a goal number 1..17). Null if absent/blank/unknown. */
    private SdgGoal resolveSdgGoal(List<String> row, Map<String, Integer> columns) {
        if (!columns.containsKey("sdggoal")) {
            return null;
        }
        String raw = value(row, columns, "sdggoal");
        if (raw.isBlank()) {
            return null;
        }
        try {
            return sdgGoalRepository.findByGoalNumber(Integer.parseInt(raw)).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

