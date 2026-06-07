package com.mathewshereni.open_aid_transparency.ingestion;

import com.mathewshereni.open_aid_transparency.donor.Donor;
import com.mathewshereni.open_aid_transparency.donor.DonorRepository;
import com.mathewshereni.open_aid_transparency.funding.FundingFlow;
import com.mathewshereni.open_aid_transparency.funding.FundingFlowRepository;
import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import com.mathewshereni.open_aid_transparency.recipient.RecipientRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.RoundingMode;

/**
 * The Spring Batch job that imports World Bank ODA figures as FundingFlow rows.
 *
 * A Job is made of Steps; this Step is "chunk-oriented": it READS items one at a
 * time, PROCESSES each, buffers them, and every CHUNK_SIZE items hands the batch
 * to the WRITER inside one transaction. Progress is recorded in the BATCH_* tables
 * so a failed run could be inspected/restarted.
 *
 * The reader and processor are @StepScope so a FRESH one is built per run, reading
 * its inputs (year range, donor id) from that run's JobParameters.
 */
@Configuration
public class OdaIngestionBatchConfig {

    private static final int CHUNK_SIZE = 100;

    /**
     * READER: pull all ODA points for the requested years into memory, then feed
     * them one by one. (Eager fetch is fine for this dataset's size; a far larger
     * source would stream page-by-page instead.)
     */
    @Bean
    @StepScope
    public ItemReader<WorldBankOdaPoint> odaReader(
            @Value("#{jobParameters['fromYear']}") Long fromYear,
            @Value("#{jobParameters['toYear']}") Long toYear,
            WorldBankOdaClient client) {
        return new ListItemReader<>(client.fetchOdaReceived(fromYear.intValue(), toYear.intValue()));
    }

    /**
     * PROCESSOR: turn one data point into a FundingFlow, or return null to DISCARD
     * it. We discard points for countries we don't track, and points we've already
     * imported - that second check is what makes re-running the job idempotent.
     */
    @Bean
    @StepScope
    public ItemProcessor<WorldBankOdaPoint, FundingFlow> odaProcessor(
            @Value("#{jobParameters['donorId']}") Long donorId,
            DonorRepository donorRepository,
            RecipientRepository recipientRepository,
            FundingFlowRepository fundingFlowRepository) {

        Donor aggregateDonor = donorRepository.findById(donorId)
                .orElseThrow(() -> new IllegalStateException("Aggregate donor " + donorId + " not found"));

        return point -> {
            Recipient recipient = recipientRepository.findByIsoCodeIgnoreCase(point.iso()).orElse(null);
            if (recipient == null) {
                return null;   // not one of our tracked recipient countries
            }
            if (fundingFlowRepository.existsByRecipientAndYearAndDonor(recipient, point.year(), aggregateDonor)) {
                return null;   // already imported on a previous run
            }
            return FundingFlow.builder()
                    .donor(aggregateDonor)
                    .recipient(recipient)
                    .currency("USD")                       // the indicator is in current US$
                    .year(point.year())
                    .amount(point.amountUsd().setScale(2, RoundingMode.HALF_UP))
                    .build();
        };
    }

    /** WRITER: persist each chunk of new flows in one go. */
    @Bean
    public ItemWriter<FundingFlow> odaWriter(FundingFlowRepository fundingFlowRepository) {
        return chunk -> fundingFlowRepository.saveAll(chunk.getItems());
    }

    @Bean
    public Step odaImportStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              ItemReader<WorldBankOdaPoint> odaReader,
                              ItemProcessor<WorldBankOdaPoint, FundingFlow> odaProcessor,
                              ItemWriter<FundingFlow> odaWriter) {
        return new StepBuilder("odaImportStep", jobRepository)
                .<WorldBankOdaPoint, FundingFlow>chunk(CHUNK_SIZE, transactionManager)
                .reader(odaReader)
                .processor(odaProcessor)
                .writer(odaWriter)
                .build();
    }

    @Bean
    public Job odaImportJob(JobRepository jobRepository, Step odaImportStep) {
        return new JobBuilder("odaImportJob", jobRepository)
                .start(odaImportStep)
                .build();
    }
}
