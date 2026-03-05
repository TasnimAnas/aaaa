package com.example.demo.config;

import com.example.demo.model.RetailSalesCsvRow;
import com.example.demo.model.RetailSalesRecord;
import com.example.demo.processor.RetailSalesProcessor;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch Configuration
 *
 * Defines the complete batch job pipeline:
 *   READER  → reads retail_sales_dataset.csv line by line
 *   PROCESSOR → transforms each row (add, subtract, multiply, divide, etc.)
 *   WRITER  → persists the transformed record to the database via JPA
 *
 * The job processes 10 records at a time (chunk size = 10).
 */
@Configuration
public class BatchConfig {

    // ─── READER: Reads CSV file into RetailSalesCsvRow objects ─────

    @Bean
    public FlatFileItemReader<RetailSalesCsvRow> reader() {
        return new FlatFileItemReaderBuilder<RetailSalesCsvRow>()
                .name("retailSalesCsvReader")
                .resource(new ClassPathResource("retail_sales_dataset.csv"))
                .linesToSkip(1) // skip CSV header row
                .delimited()
                .names("transactionId", "date", "customerId", "gender", "age",
                        "productCategory", "quantity", "pricePerUnit", "totalAmount")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(RetailSalesCsvRow.class);
                }})
                .build();
    }

    // ─── PROCESSOR: Transforms CSV rows into enriched DB records ───

    @Bean
    public RetailSalesProcessor processor() {
        return new RetailSalesProcessor();
    }

    // ─── WRITER: Persists transformed records to database via JPA ──

    @Bean
    public JpaItemWriter<RetailSalesRecord> writer(EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<RetailSalesRecord>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    // ─── STEP: A single step that reads → processes → writes ───────

    @Bean
    public Step step1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      FlatFileItemReader<RetailSalesCsvRow> reader,
                      RetailSalesProcessor processor,
                      JpaItemWriter<RetailSalesRecord> writer) {

        return new StepBuilder("csvToDbStep", jobRepository)
                .<RetailSalesCsvRow, RetailSalesRecord>chunk(10)
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    // ─── JOB: The batch job that orchestrates the step(s) ──────────

    @Bean
    public Job importRetailSalesJob(JobRepository jobRepository, Step step1) {
        return new JobBuilder("importRetailSalesJob", jobRepository)
                .start(step1)
                .build();
    }
}
