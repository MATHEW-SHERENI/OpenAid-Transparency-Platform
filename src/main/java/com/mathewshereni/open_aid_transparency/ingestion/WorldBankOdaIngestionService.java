package com.mathewshereni.open_aid_transparency.ingestion;

import com.mathewshereni.open_aid_transparency.donor.Donor;
import com.mathewshereni.open_aid_transparency.donor.DonorRepository;
import com.mathewshereni.open_aid_transparency.donor.DonorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Service;

/**
 * Launches the ODA import job. Spring Batch refuses to run the same job with the
 * SAME parameters twice, so we add a unique run.id - this lets an admin re-run the
 * import (it stays correct because the processor skips already-imported flows).
 *
 * The World Bank "ODA received" figure is an aggregate with no single donor, so we
 * attach every imported flow to one synthetic "aggregate" donor, created on first run.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorldBankOdaIngestionService {

    public static final String AGGREGATE_DONOR_NAME = "Official Development Assistance (aggregate)";

    private final JobLauncher jobLauncher;
    private final Job odaImportJob;
    private final DonorRepository donorRepository;

    public IngestionResult importOda(int fromYear, int toYear) throws Exception {
        Donor donor = donorRepository.findByNameIgnoreCase(AGGREGATE_DONOR_NAME)
                .orElseGet(() -> donorRepository.save(Donor.builder()
                        .name(AGGREGATE_DONOR_NAME)
                        .type(DonorType.MULTILATERAL)
                        .build()));

        JobParameters parameters = new JobParametersBuilder()
                .addLong("donorId", donor.getId())
                .addLong("fromYear", (long) fromYear)
                .addLong("toYear", (long) toYear)
                .addLong("run.id", System.currentTimeMillis())   // makes each launch unique
                .toJobParameters();

        JobExecution execution = jobLauncher.run(odaImportJob, parameters);

        // The job has one step; its counters map cleanly onto our summary:
        //   read   = points fetched, write = flows created, filter = points skipped.
        StepExecution step = execution.getStepExecutions().iterator().next();
        IngestionResult result = new IngestionResult(
                (int) step.getReadCount(),
                (int) step.getWriteCount(),
                (int) step.getFilterCount());
        log.info("ODA import finished: {}", result);
        return result;
    }
}
