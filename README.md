# Spring Batch Retail Sales Data Transformer — POC

A **Proof of Concept** demonstrating **Spring Batch** reading a CSV dataset, applying arithmetic and logical transformations on each row, and writing the enriched data into a SQL database.

---

## What This Project Does

```
retail_sales_dataset.csv  →  FlatFileItemReader  →  RetailSalesProcessor  →  JpaItemWriter  →  H2 SQL Database
        (1000 rows)             (reads CSV)          (transforms data)        (persists)        (retail_sales table)
```

The batch job reads **1000 retail sales records** from a CSV file, computes **12 new columns** using various operations, and inserts the enriched records (21 columns total) into an in-memory H2 database.

---

## Dataset

**Source:** [Kaggle — Retail Sales Dataset](https://www.kaggle.com/datasets/mohammadtalib786/retail-sales-dataset)  
**File:** `retail_sales_dataset.csv`  
**Rows:** 1000 data rows + 1 header row

### CSV Columns (9 Original)

| Column           | Type    | Example                       | Description                   |
| ---------------- | ------- | ----------------------------- | ----------------------------- |
| Transaction ID   | Long    | 1                             | Unique transaction identifier |
| Date             | String  | 2023-11-24                    | Transaction date              |
| Customer ID      | String  | CUST001                       | Unique customer identifier    |
| Gender           | String  | Male / Female                 | Customer gender               |
| Age              | Integer | 34                            | Customer age                  |
| Product Category | String  | Beauty, Clothing, Electronics | Product type                  |
| Quantity         | Integer | 3                             | Units purchased               |
| Price per Unit   | Double  | 50                            | Unit price in dollars         |
| Total Amount     | Double  | 150                           | Total transaction value       |

---

## Transformations (12 Computed Columns)

The `RetailSalesProcessor` applies these operations to every row:

| #   | Operation          | Output Column           | Formula                                                     |
| --- | ------------------ | ----------------------- | ----------------------------------------------------------- |
| 1   | **Addition**       | `price_with_tax`        | `pricePerUnit + (pricePerUnit × 0.18)` — adds 18% GST       |
| 2   | **Addition**       | `quantity_with_bonus`   | `quantity + 5` — adds 5 bonus items                         |
| 3   | **Subtraction**    | `discounted_amount`     | `totalAmount - (totalAmount × 0.10)` — removes 10% discount |
| 4   | **Subtraction**    | `price_after_coupon`    | `max(pricePerUnit - 10, 0)` — flat $10 coupon               |
| 5   | **Multiplication** | `total_amount_doubled`  | `totalAmount × 2` — simulates double-order                  |
| 6   | **Multiplication** | `total_with_tax`        | `totalAmount × 1.18` — total including 18% GST              |
| 7   | **Division**       | `avg_price_per_unit`    | `totalAmount / quantity` — average price per item           |
| 8   | **Division**       | `spending_per_age_year` | `totalAmount / age` — spending per year of life             |
| 9   | **Modulo**         | `quantity_parity`       | `quantity % 2` → "Even" or "Odd"                            |
| 10  | **Conditional**    | `age_group`             | ≤25 → "Young", 26–45 → "Middle-Aged", 46+ → "Senior"        |
| 11  | **Conditional**    | `spending_tier`         | <100 → "Low", 100–500 → "Medium", >500 → "High"             |
| 12  | **Combined**       | `estimated_profit`      | `totalAmount × 0.30` — 30% profit margin estimate           |

---

## Technologies Used

| Technology      | Version            | Purpose                                        |
| --------------- | ------------------ | ---------------------------------------------- |
| Java            | 17                 | Programming language                           |
| Spring Boot     | 4.0.3              | Application framework                          |
| Spring Batch    | 6.0.2              | Batch processing (Reader → Processor → Writer) |
| Spring Data JPA | (managed)          | ORM layer for database persistence             |
| Hibernate       | 7.2.4.Final        | JPA implementation, auto-creates DB tables     |
| H2 Database     | (managed)          | In-memory SQL database                         |
| Lombok          | (managed)          | Reduces boilerplate code                       |
| Maven           | Wrapper included   | Build tool                                     |
| Tomcat          | 11.0.18 (embedded) | Web server for H2 console                      |

---

## Project Structure

```
demo/
├── pom.xml                                          # Maven build config & dependencies
├── mvnw / mvnw.cmd                                  # Maven wrapper (no Maven install needed)
├── retail_sales_dataset.csv                          # Original dataset (root copy)
├── README.md                                        # This file
│
└── src/
    └── main/
        ├── java/com/example/demo/
        │   ├── DemoApplication.java                  # Spring Boot entry point
        │   │
        │   ├── config/
        │   │   └── BatchConfig.java                  # Batch job configuration
        │   │                                         #   (Reader, Processor, Writer, Step, Job)
        │   │
        │   ├── model/
        │   │   ├── RetailSalesCsvRow.java             # INPUT DTO: maps to CSV columns (9 fields)
        │   │   └── RetailSalesRecord.java             # OUTPUT JPA Entity: maps to DB table (21 cols)
        │   │
        │   └── processor/
        │       └── RetailSalesProcessor.java          # ItemProcessor: all 12 transformations
        │
        └── resources/
            ├── application.properties                 # DB, JPA, Batch, logging configuration
            └── retail_sales_dataset.csv               # Dataset on classpath (read by Reader)
```

### File Details

| File                        | Lines | Role                                                                                                                 |
| --------------------------- | ----- | -------------------------------------------------------------------------------------------------------------------- |
| `DemoApplication.java`      | 13    | `@SpringBootApplication` — boots the app, triggers batch job on startup                                              |
| `RetailSalesCsvRow.java`    | 30    | Plain DTO with 9 fields matching CSV headers. Used by `FlatFileItemReader`                                           |
| `RetailSalesRecord.java`    | 138   | JPA `@Entity` with 21 columns (9 original + 12 computed). Hibernate auto-creates the `retail_sales` table from this  |
| `RetailSalesProcessor.java` | 135   | Implements `ItemProcessor<RetailSalesCsvRow, RetailSalesRecord>`. Contains all 12 transformations and helper methods |
| `BatchConfig.java`          | 96    | `@Configuration` class defining 5 beans: `reader()`, `processor()`, `writer()`, `step1()`, `importRetailSalesJob()`  |
| `application.properties`    | 26    | H2 database URL, JPA settings, H2 console, batch auto-run config, logging levels                                     |
| `pom.xml`                   | 117   | Maven config: Spring Boot 4.0.3 parent, 8 dependencies, Lombok annotation processor                                  |

---

## What Gets Generated at Runtime

When the application starts, these things happen automatically:

1. **H2 in-memory database** `retaildb` is created
2. **Spring Batch metadata tables** are auto-created (`BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION`, etc.)
3. **`retail_sales` table** is auto-created by Hibernate with 21 columns
4. **Batch job `importRetailSalesJob`** executes:
   - Reads 1000 CSV rows in chunks of 10
   - Applies 12 transformations per row
   - Inserts 1000 enriched records into `retail_sales`
5. Job completes in ~200ms with status **`COMPLETED`**

### Output Table Schema

| Original Columns (from CSV) | Computed Columns (by Processor) |
| --------------------------- | ------------------------------- |
| `transaction_id` (PK)       | `price_with_tax`                |
| `transaction_date`          | `discounted_amount`             |
| `customer_id`               | `total_amount_doubled`          |
| `gender`                    | `avg_price_per_unit`            |
| `age`                       | `quantity_with_bonus`           |
| `product_category`          | `price_after_coupon`            |
| `quantity`                  | `total_with_tax`                |
| `price_per_unit`            | `spending_per_age_year`         |
| `total_amount`              | `age_group`                     |
|                             | `spending_tier`                 |
|                             | `quantity_parity`               |
|                             | `estimated_profit`              |

---

## Requirements

| Requirement  | Details                                                     |
| ------------ | ----------------------------------------------------------- |
| **Java**     | JDK 17 or higher (must be on `PATH`)                        |
| **Maven**    | Not required — Maven Wrapper (`mvnw`) is included           |
| **OS**       | macOS, Linux, or Windows                                    |
| **Database** | None — H2 runs in-memory, no installation needed            |
| **Network**  | Needed only on first build (to download dependencies)       |
| **Dataset**  | `retail_sales_dataset.csv` must be in `src/main/resources/` |

---

## How to Run

### Step 1: Verify Java

```bash
java -version    # Should show 17+
```

### Step 2: Build

```bash
cd /path/to/demo
./mvnw clean compile
```

### Step 3: Run

```bash
./mvnw spring-boot:run
```

### Step 4: Verify Job Completed

Look for this line in the console:

```
Job: [SimpleJob: [name=importRetailSalesJob]] completed with status: [COMPLETED]
```

### Step 5: Browse Data via H2 Console

1. Open **http://localhost:8080/h2-console** in your browser
2. Enter connection details:
   - **JDBC URL:** `jdbc:h2:mem:retaildb`
   - **Username:** `sa`
   - **Password:** _(leave empty)_
3. Click **Connect**
4. Run queries:

```sql
-- View all transformed data
SELECT * FROM retail_sales LIMIT 20;

-- View only computed columns
SELECT transaction_id, price_with_tax, discounted_amount,
       total_amount_doubled, avg_price_per_unit, age_group,
       spending_tier, estimated_profit
FROM retail_sales LIMIT 10;

-- Count total records
SELECT COUNT(*) FROM retail_sales;

-- Group by age group
SELECT age_group, COUNT(*), AVG(total_amount)
FROM retail_sales GROUP BY age_group;

-- Group by spending tier
SELECT spending_tier, COUNT(*), SUM(estimated_profit)
FROM retail_sales GROUP BY spending_tier;
```

### Step 6: Stop

```
Ctrl + C
```

---

## Spring Batch Concepts Demonstrated

| Concept              | Where                                         | What It Does                                  |
| -------------------- | --------------------------------------------- | --------------------------------------------- |
| **Job**              | `BatchConfig.importRetailSalesJob()`          | Top-level container that orchestrates steps   |
| **Step**             | `BatchConfig.step1()`                         | A single unit of work: read → process → write |
| **ItemReader**       | `BatchConfig.reader()` → `FlatFileItemReader` | Reads CSV file line-by-line, maps to DTO      |
| **ItemProcessor**    | `RetailSalesProcessor.process()`              | Transforms input into enriched output         |
| **ItemWriter**       | `BatchConfig.writer()` → `JpaItemWriter`      | Persists output to database via JPA           |
| **Chunk Processing** | `chunk(10)` in step config                    | Processes 10 records per database transaction |
| **JobRepository**    | Auto-configured                               | Stores job/step execution metadata in H2      |
| **Auto-launch**      | `spring.batch.job.enabled=true`               | Job runs automatically when app starts        |
