# Spring Batch — Complete Explanation (Beginner-Friendly)

This document explains **everything** about this project from scratch — what Spring Batch is, how it works, and a line-by-line explanation of every file in the project.

---

## Table of Contents

1. [What is Batch Processing?](#1-what-is-batch-processing)
2. [What is Spring Batch?](#2-what-is-spring-batch)
3. [Core Concepts of Spring Batch](#3-core-concepts-of-spring-batch)
4. [How Spring Batch Works in This Project](#4-how-spring-batch-works-in-this-project)
5. [Code Explanation — Every File](#5-code-explanation--every-file)
   - [5.1 pom.xml](#51-pomxml--dependencies)
   - [5.2 application.properties](#52-applicationproperties--configuration)
   - [5.3 DemoApplication.java](#53-demoapplicationjava--entry-point)
   - [5.4 RetailSalesCsvRow.java](#54-retailsalescsvrowjava--input-dto)
   - [5.5 RetailSalesRecord.java](#55-retailsalesrecordjava--output-entity)
   - [5.6 RetailSalesProcessor.java](#56-retailsalesprocessorjava--transformation-logic)
   - [5.7 BatchConfig.java](#57-batchconfigjava--batch-job-configuration)
6. [What Happens When You Run the App](#6-what-happens-when-you-run-the-app)
7. [Key Terms Glossary](#7-key-terms-glossary)

---

## 1. What is Batch Processing?

**Batch processing** means processing a **large amount of data** in one go, without user interaction.

### Real-World Examples:

- A bank processes **millions of transactions** at the end of each day
- A company generates **monthly salary slips** for all 10,000 employees
- An e-commerce site **imports product data** from a CSV file into a database

### How is it different from normal processing?

| Normal (Real-time) Processing      | Batch Processing                         |
| ---------------------------------- | ---------------------------------------- |
| User clicks a button → gets result | No user interaction; runs automatically  |
| Handles 1 request at a time        | Handles thousands/millions of records    |
| Example: Searching a product       | Example: Importing 1000 CSV rows into DB |

---

## 2. What is Spring Batch?

**Spring Batch** is a Java framework built on top of Spring that makes it easy to write batch processing applications.

Think of it like an **assembly line in a factory**:

```
Raw Material  →  Worker picks it up  →  Worker processes it  →  Worker puts it in a box
   (CSV row)        (Reader)              (Processor)              (Writer)
```

Spring Batch gives you **ready-made components** for:

- **Reading** data from files, databases, queues
- **Processing** (transforming, validating, enriching) each record
- **Writing** data to files, databases, queues
- **Error handling** — skip bad records, retry failures
- **Job tracking** — knows which jobs ran, when, and whether they succeeded

### Why not just write a for-loop?

You _could_ read a CSV with a for-loop and insert rows one by one. But Spring Batch gives you:

| Feature                | DIY For-Loop       | Spring Batch                           |
| ---------------------- | ------------------ | -------------------------------------- |
| Transaction management | Manual             | Automatic (commits every N records)    |
| Error recovery         | Manual try-catch   | Built-in skip/retry policies           |
| Restart after failure  | Start from scratch | Resumes from where it stopped          |
| Job history/logging    | None               | Full metadata stored in DB             |
| Scalability            | Single-threaded    | Supports multi-threading, partitioning |

---

## 3. Core Concepts of Spring Batch

Spring Batch has a simple hierarchy:

```
JOB
 └── STEP (one or more)
      ├── READER    → reads one item at a time
      ├── PROCESSOR → transforms the item
      └── WRITER    → writes a chunk of items
```

### 3.1 Job

A **Job** is the entire batch operation. It's like saying "Import all retail sales data."

```java
// Example: Define a job
Job importJob = new JobBuilder("importRetailSalesJob", jobRepository)
    .start(step1)     // Start with step1
    .build();
```

A Job can have multiple Steps executed in sequence.

### 3.2 Step

A **Step** is a single phase of the job. Most steps follow the **Read-Process-Write** pattern.

```java
// Example: Define a step
Step step1 = new StepBuilder("csvToDbStep", jobRepository)
    .chunk(10)          // Process 10 records at a time
    .reader(reader)     // Where to read from
    .processor(proc)    // How to transform
    .writer(writer)     // Where to write to
    .build();
```

### 3.3 Chunk Processing

Instead of processing records one-by-one, Spring Batch groups them into **chunks**.

With `chunk(10)`:

```
Read item 1
Read item 2
...
Read item 10
Process item 1 → Process item 2 → ... → Process item 10
Write all 10 items to database in ONE transaction
──────────────────────────────────────────────────────
Read item 11
Read item 12
...
```

**Why chunks?** Writing to a database for every single record is slow. Batching writes (e.g., 10 at a time) is much faster and safer — if something fails, only the current chunk is rolled back.

### 3.4 ItemReader

Reads data **one item at a time** from a source. Spring Batch provides many built-in readers:

| Reader                 | Reads From                       |
| ---------------------- | -------------------------------- |
| `FlatFileItemReader`   | CSV, TSV, fixed-width text files |
| `JdbcCursorItemReader` | SQL database via JDBC            |
| `JpaPagingItemReader`  | Database via JPA                 |
| `JsonItemReader`       | JSON files                       |

In this project, we use `FlatFileItemReader` to read the CSV.

### 3.5 ItemProcessor

Transforms each item. Takes an **input object** and returns an **output object** (can be a different type).

```java
// Simple example:
public class MyProcessor implements ItemProcessor<InputType, OutputType> {
    public OutputType process(InputType input) {
        OutputType output = new OutputType();
        output.setName(input.getName().toUpperCase()); // transform!
        return output;
    }
}
```

If the processor returns `null`, the item is **skipped** (not written).

### 3.6 ItemWriter

Writes a **list of items** (one chunk) to the destination. Built-in writers include:

| Writer                | Writes To                  |
| --------------------- | -------------------------- |
| `JpaItemWriter`       | Database via JPA/Hibernate |
| `JdbcBatchItemWriter` | Database via JDBC          |
| `FlatFileItemWriter`  | CSV/text files             |
| `JsonFileItemWriter`  | JSON files                 |

In this project, we use `JpaItemWriter` to insert records into H2 database.

### 3.7 JobRepository

The **JobRepository** stores metadata about every job execution:

- When did it start/end?
- How many records were read/written/skipped?
- Did it succeed or fail?

This allows Spring Batch to **resume failed jobs** from where they stopped.

---

## 4. How Spring Batch Works in This Project

### The Big Picture

```
┌──────────────────────────────────────────────────────────────┐
│                    importRetailSalesJob                       │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                    csvToDbStep                          │  │
│  │                                                        │  │
│  │  ┌──────────┐   ┌─────────────────┐   ┌────────────┐  │  │
│  │  │  READER   │──→│   PROCESSOR     │──→│   WRITER   │  │  │
│  │  │          │   │                 │   │            │  │  │
│  │  │ Reads CSV│   │ Adds columns:  │   │ Inserts to │  │  │
│  │  │ file row │   │ +tax, -discount│   │ H2 database│  │  │
│  │  │ by row   │   │ ×2, ÷quantity  │   │ via JPA    │  │  │
│  │  └──────────┘   └─────────────────┘   └────────────┘  │  │
│  │                                                        │  │
│  │              chunk size = 10 records                    │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Step-by-Step Flow

1. **App starts** → Spring Boot boots up, creates H2 database
2. **Spring Batch auto-runs** the job (`spring.batch.job.enabled=true`)
3. **Reader** opens `retail_sales_dataset.csv`, skips the header row
4. **Reader** reads row 1 → creates `RetailSalesCsvRow` object with 9 fields
5. **Processor** receives the object → computes 12 new fields → returns `RetailSalesRecord` with 21 fields
6. Steps 4–5 repeat for rows 2–10 (chunk size = 10)
7. **Writer** receives all 10 `RetailSalesRecord` objects → inserts them into `retail_sales` table
8. Steps 4–7 repeat until all 1000 rows are processed (100 chunks total)
9. **Job completes** with status `COMPLETED`

---

## 5. Code Explanation — Every File

---

### 5.1 `pom.xml` — Dependencies

The `pom.xml` is the **Maven build file**. It tells Maven what libraries (dependencies) the project needs.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.3</version>
</parent>
```

This says: "My project is based on **Spring Boot 4.0.3**." The parent manages all library versions so we don't have to specify them manually.

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

This tells Maven to compile with **Java 17**.

#### Key Dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
```

**Spring Batch starter** — brings in the entire Spring Batch framework (Job, Step, ItemReader, ItemProcessor, ItemWriter, JobRepository, etc.).

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

**Spring Data JPA** — lets us use `@Entity` annotations and Hibernate to auto-create database tables and insert data.

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**H2 Database** — a lightweight, in-memory SQL database. Perfect for POC because it needs no installation. Data lives in memory and disappears when the app stops.

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**Lombok** — generates boilerplate code (getters, setters, constructors, `toString`) at compile time using annotations like `@Data`.

---

### 5.2 `application.properties` — Configuration

This file configures the entire application.

```properties
spring.application.name=demo
```

The name of the application.

```properties
spring.datasource.url=jdbc:h2:mem:retaildb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

**Database connection settings:**

- `jdbc:h2:mem:retaildb` → Use H2, in-memory mode, database named "retaildb"
- `org.h2.Driver` → The JDBC driver class for H2
- `sa` → Default username (System Admin), no password

```properties
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

**JPA/Hibernate settings:**

- `H2Dialect` → Tells Hibernate to generate H2-compatible SQL
- `ddl-auto=update` → Hibernate auto-creates/updates tables based on `@Entity` classes
- `show-sql=true` → Prints every SQL statement to the console (useful for debugging)

```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**H2 Console:** Enables a web-based database browser at `http://localhost:8080/h2-console`.

```properties
spring.batch.jdbc.initialize-schema=always
spring.batch.job.enabled=true
```

**Spring Batch settings:**

- `initialize-schema=always` → Create Spring Batch metadata tables automatically
- `job.enabled=true` → Run the batch job automatically when the app starts

---

### 5.3 `DemoApplication.java` — Entry Point

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

**Line by line:**

- `@SpringBootApplication` — This is a shortcut for three annotations combined:
  - `@Configuration` → This class can define beans
  - `@EnableAutoConfiguration` → Spring Boot auto-configures everything based on dependencies
  - `@ComponentScan` → Scans for `@Component`, `@Service`, `@Configuration` classes in this package and sub-packages
- `SpringApplication.run(...)` — Starts the Spring Boot application. This triggers:
  1. Dependency injection (creates all beans)
  2. Database connection
  3. Hibernate table creation
  4. Batch job execution

---

### 5.4 `RetailSalesCsvRow.java` — Input DTO

```java
package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetailSalesCsvRow {
    private Long transactionId;
    private String date;
    private String customerId;
    private String gender;
    private Integer age;
    private String productCategory;
    private Integer quantity;
    private Double pricePerUnit;
    private Double totalAmount;
}
```

**What is a DTO?**  
DTO = **Data Transfer Object**. It's a simple class that only holds data — no business logic. Think of it as a "container" to carry data from one place to another.

**What is this class for?**  
When the `FlatFileItemReader` reads one line of the CSV:

```
1,2023-11-24,CUST001,Male,34,Beauty,3,50,150
```

It creates a `RetailSalesCsvRow` object and fills in:

```java
transactionId = 1
date = "2023-11-24"
customerId = "CUST001"
gender = "Male"
age = 34
productCategory = "Beauty"
quantity = 3
pricePerUnit = 50.0
totalAmount = 150.0
```

**Lombok Annotations:**

| Annotation            | What It Generates                                                       |
| --------------------- | ----------------------------------------------------------------------- |
| `@Data`               | Getters, Setters, `toString()`, `equals()`, `hashCode()` for all fields |
| `@NoArgsConstructor`  | Empty constructor: `new RetailSalesCsvRow()`                            |
| `@AllArgsConstructor` | Constructor with all 9 parameters                                       |

Without Lombok, you'd have to write ~80 lines of boilerplate code for this class. With Lombok, it's 9 lines.

---

### 5.5 `RetailSalesRecord.java` — Output Entity

This is the **output object** and also a **JPA Entity** (maps to a database table).

```java
@Entity
@Table(name = "retail_sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetailSalesRecord {
```

**Key annotations:**

- `@Entity` — Tells Hibernate: "This class represents a database table"
- `@Table(name = "retail_sales")` — The table name in the database will be `retail_sales`

**Original columns** (copied from CSV):

```java
@Id
@Column(name = "transaction_id")
private Long transactionId;
```

- `@Id` — This is the **primary key** of the table
- `@Column(name = "transaction_id")` — Maps this Java field to the DB column `transaction_id`

**Computed columns** (calculated by the Processor):

```java
@Column(name = "price_with_tax")
private Double priceWithTax;         // ADDITION: price + 18% GST

@Column(name = "discounted_amount")
private Double discountedAmount;     // SUBTRACTION: total - 10%

@Column(name = "total_amount_doubled")
private Double totalAmountDoubled;   // MULTIPLICATION: total × 2

@Column(name = "avg_price_per_unit")
private Double avgPricePerUnit;      // DIVISION: total ÷ quantity

@Column(name = "age_group")
private String ageGroup;             // CONDITIONAL: Young/Middle-Aged/Senior

@Column(name = "quantity_parity")
private String quantityParity;       // MODULO: Even/Odd
```

**How does Hibernate auto-create the table?**

Because `spring.jpa.hibernate.ddl-auto=update` is set, Hibernate reads all `@Entity` classes and generates SQL:

```sql
CREATE TABLE retail_sales (
    transaction_id BIGINT NOT NULL PRIMARY KEY,
    transaction_date VARCHAR(255),
    customer_id VARCHAR(255),
    gender VARCHAR(255),
    age INTEGER,
    product_category VARCHAR(255),
    quantity INTEGER,
    price_per_unit FLOAT,
    total_amount FLOAT,
    price_with_tax FLOAT,
    discounted_amount FLOAT,
    total_amount_doubled FLOAT,
    avg_price_per_unit FLOAT,
    quantity_with_bonus INTEGER,
    price_after_coupon FLOAT,
    total_with_tax FLOAT,
    spending_per_age_year FLOAT,
    age_group VARCHAR(255),
    spending_tier VARCHAR(255),
    quantity_parity VARCHAR(255),
    estimated_profit FLOAT
);
```

You never write this SQL — Hibernate generates it from your Java class!

---

### 5.6 `RetailSalesProcessor.java` — Transformation Logic

This is the **heart of the project**. It takes each CSV row and computes 12 new columns.

```java
@Slf4j
@Component
public class RetailSalesProcessor implements ItemProcessor<RetailSalesCsvRow, RetailSalesRecord> {
```

- `@Slf4j` — Lombok annotation that creates a logger. You can use `log.info(...)` to print messages
- `@Component` — Tells Spring to auto-detect and create this bean
- `implements ItemProcessor<INPUT, OUTPUT>` — Spring Batch interface. Input is `RetailSalesCsvRow`, output is `RetailSalesRecord`

The `process()` method is called **once for every CSV row**.

#### Transformation 1: ADDITION — Price with Tax

```java
double tax = csv.getPricePerUnit() * 0.18;
record.setPriceWithTax(roundToTwo(csv.getPricePerUnit() + tax));
```

**What it does:** Adds 18% GST to the unit price.

**Example:**

```
Input:  pricePerUnit = 50.0
Tax:    50.0 × 0.18 = 9.0
Output: priceWithTax = 50.0 + 9.0 = 59.0
```

#### Transformation 2: ADDITION — Quantity with Bonus

```java
record.setQuantityWithBonus(csv.getQuantity() + 5);
```

**Example:**

```
Input:  quantity = 3
Output: quantityWithBonus = 3 + 5 = 8
```

#### Transformation 3: SUBTRACTION — Discounted Amount

```java
double discount = csv.getTotalAmount() * 0.10;
record.setDiscountedAmount(roundToTwo(csv.getTotalAmount() - discount));
```

**What it does:** Removes a 10% discount from the total amount.

**Example:**

```
Input:  totalAmount = 1000.0
Discount: 1000.0 × 0.10 = 100.0
Output: discountedAmount = 1000.0 - 100.0 = 900.0
```

#### Transformation 4: SUBTRACTION — Price After Coupon

```java
record.setPriceAfterCoupon(roundToTwo(Math.max(csv.getPricePerUnit() - 10.0, 0.0)));
```

**What it does:** Subtracts a flat $10 coupon. Uses `Math.max()` to ensure it never goes below 0.

**Example:**

```
Input:  pricePerUnit = 50.0  →  Output: 50 - 10 = 40.0
Input:  pricePerUnit = 5.0   →  Output: max(5 - 10, 0) = max(-5, 0) = 0.0
```

#### Transformation 5: MULTIPLICATION — Total Doubled

```java
record.setTotalAmountDoubled(roundToTwo(csv.getTotalAmount() * 2.0));
```

**Example:**

```
Input:  totalAmount = 150.0
Output: totalAmountDoubled = 150.0 × 2 = 300.0
```

#### Transformation 6: MULTIPLICATION — Total with Tax

```java
record.setTotalWithTax(roundToTwo(csv.getTotalAmount() * 1.18));
```

**What it does:** Multiplies the total by 1.18 (adds 18% GST to the total).

**Example:**

```
Input:  totalAmount = 1000.0
Output: totalWithTax = 1000.0 × 1.18 = 1180.0
```

#### Transformation 7: DIVISION — Average Price per Unit

```java
if (csv.getQuantity() != null && csv.getQuantity() > 0) {
    record.setAvgPricePerUnit(roundToTwo(csv.getTotalAmount() / csv.getQuantity()));
} else {
    record.setAvgPricePerUnit(0.0);
}
```

**What it does:** Divides total by quantity. Checks for zero/null to avoid `ArithmeticException`.

**Example:**

```
Input:  totalAmount = 150.0, quantity = 3
Output: avgPricePerUnit = 150.0 ÷ 3 = 50.0
```

#### Transformation 8: DIVISION — Spending per Age Year

```java
if (csv.getAge() != null && csv.getAge() > 0) {
    record.setSpendingPerAgeYear(roundToTwo(csv.getTotalAmount() / csv.getAge()));
}
```

**Example:**

```
Input:  totalAmount = 150.0, age = 34
Output: spendingPerAgeYear = 150.0 ÷ 34 = 4.41
```

#### Transformation 9: MODULO — Quantity Parity

```java
record.setQuantityParity(csv.getQuantity() % 2 == 0 ? "Even" : "Odd");
```

**What is modulo?** The `%` operator gives the **remainder** of division.

**Example:**

```
4 % 2 = 0 → "Even"
3 % 2 = 1 → "Odd"
7 % 2 = 1 → "Odd"
```

The `? :` is a **ternary operator** — a compact if-else:

```java
// These two are equivalent:
result = (condition) ? valueIfTrue : valueIfFalse;

if (condition) result = valueIfTrue;
else result = valueIfFalse;
```

#### Transformation 10: CONDITIONAL — Age Group

```java
private String classifyAgeGroup(Integer age) {
    if (age == null) return "Unknown";
    if (age <= 25) return "Young";
    if (age <= 45) return "Middle-Aged";
    return "Senior";
}
```

**Example:**

```
age = 20  → "Young"
age = 34  → "Middle-Aged"
age = 50  → "Senior"
```

#### Transformation 11: CONDITIONAL — Spending Tier

```java
private String classifySpendingTier(Double totalAmount) {
    if (totalAmount == null) return "Unknown";
    if (totalAmount < 100) return "Low";
    if (totalAmount <= 500) return "Medium";
    return "High";
}
```

**Example:**

```
totalAmount = 30    → "Low"
totalAmount = 150   → "Medium"
totalAmount = 1000  → "High"
```

#### Transformation 12: COMBINED — Estimated Profit

```java
record.setEstimatedProfit(roundToTwo(csv.getTotalAmount() * 0.30));
```

**Example:**

```
Input:  totalAmount = 1000.0
Output: estimatedProfit = 1000.0 × 0.30 = 300.0
```

#### Helper: Rounding

```java
private double roundToTwo(double value) {
    return Math.round(value * 100.0) / 100.0;
}
```

**What it does:** Rounds to 2 decimal places.

**Example:**

```
Input:  4.41176470588
Step 1: 4.41176... × 100 = 441.176...
Step 2: Math.round(441.176) = 441
Step 3: 441 / 100.0 = 4.41
```

---

### 5.7 `BatchConfig.java` — Batch Job Configuration

This file **wires everything together**: Reader, Processor, Writer, Step, and Job.

```java
@Configuration
public class BatchConfig {
```

`@Configuration` tells Spring: "This class contains `@Bean` definitions that should be registered in the application context."

#### The Reader Bean

```java
@Bean
public FlatFileItemReader<RetailSalesCsvRow> reader() {
    return new FlatFileItemReaderBuilder<RetailSalesCsvRow>()
            .name("retailSalesCsvReader")
            .resource(new ClassPathResource("retail_sales_dataset.csv"))
            .linesToSkip(1)
            .delimited()
            .names("transactionId", "date", "customerId", "gender", "age",
                    "productCategory", "quantity", "pricePerUnit", "totalAmount")
            .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                setTargetType(RetailSalesCsvRow.class);
            }})
            .build();
}
```

**Line by line:**

| Code                                                           | What It Does                                                |
| -------------------------------------------------------------- | ----------------------------------------------------------- |
| `@Bean`                                                        | Registers this method's return value as a Spring bean       |
| `.name("retailSalesCsvReader")`                                | Names the reader (for logging/tracking)                     |
| `.resource(new ClassPathResource("retail_sales_dataset.csv"))` | Points to the CSV file in `src/main/resources/`             |
| `.linesToSkip(1)`                                              | Skips the first line (the CSV header row)                   |
| `.delimited()`                                                 | The file is comma-delimited (CSV)                           |
| `.names(...)`                                                  | Maps CSV columns (by position) to Java field names          |
| `.fieldSetMapper(...)`                                         | Converts each parsed line into a `RetailSalesCsvRow` object |
| `.build()`                                                     | Creates the reader                                          |

**How column mapping works:**

```
CSV Line:    1,2023-11-24,CUST001,Male,34,Beauty,3,50,150
              ↓         ↓       ↓    ↓   ↓       ↓ ↓  ↓   ↓
names:    transactionId date customerId gender age productCategory quantity pricePerUnit totalAmount
```

#### The Processor Bean

```java
@Bean
public RetailSalesProcessor processor() {
    return new RetailSalesProcessor();
}
```

Simply creates an instance of our processor class.

#### The Writer Bean

```java
@Bean
public JpaItemWriter<RetailSalesRecord> writer(EntityManagerFactory entityManagerFactory) {
    return new JpaItemWriterBuilder<RetailSalesRecord>()
            .entityManagerFactory(entityManagerFactory)
            .build();
}
```

**What is `EntityManagerFactory`?**  
It's the JPA/Hibernate component that manages database connections and operations. Spring auto-creates it based on `application.properties`.

**What does `JpaItemWriter` do?**  
For each chunk of `RetailSalesRecord` objects, it calls `entityManager.merge(record)` — which inserts or updates the record in the `retail_sales` table.

#### The Step Bean

```java
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
```

**Key parts:**

| Code                                               | What It Does                                         |
| -------------------------------------------------- | ---------------------------------------------------- |
| `"csvToDbStep"`                                    | Name of the step                                     |
| `jobRepository`                                    | Where to store step execution metadata               |
| `.<RetailSalesCsvRow, RetailSalesRecord>chunk(10)` | Process 10 items per chunk. Input type → Output type |
| `.transactionManager(transactionManager)`          | Manages DB transactions (commit/rollback)            |
| `.reader(reader)`                                  | Use our CSV reader                                   |
| `.processor(processor)`                            | Use our transformer                                  |
| `.writer(writer)`                                  | Use our JPA writer                                   |

**Chunk(10) in action:**

```
Chunk 1: Read rows 1-10  → Process rows 1-10  → Write rows 1-10  → COMMIT
Chunk 2: Read rows 11-20 → Process rows 11-20 → Write rows 11-20 → COMMIT
...
Chunk 100: Read rows 991-1000 → Process → Write → COMMIT
```

#### The Job Bean

```java
@Bean
public Job importRetailSalesJob(JobRepository jobRepository, Step step1) {
    return new JobBuilder("importRetailSalesJob", jobRepository)
            .start(step1)
            .build();
}
```

This creates a job named `importRetailSalesJob` that starts with `step1`. You can chain multiple steps:

```java
// Example with multiple steps (not in this project):
.start(step1)
.next(step2)
.next(step3)
.build();
```

---

## 6. What Happens When You Run the App

Here's the **exact sequence** from `./mvnw spring-boot:run` to job completion:

```
1. Maven compiles Java files → target/classes/
2. Spring Boot starts (DemoApplication.main())
3. Component scan finds: BatchConfig, RetailSalesProcessor
4. H2 in-memory database "retaildb" is created
5. Hibernate reads @Entity classes → creates "retail_sales" table
6. Spring Batch creates metadata tables (BATCH_JOB_INSTANCE, etc.)
7. Spring Batch creates beans: reader, processor, writer, step1, job
8. JobLauncherApplicationRunner auto-launches "importRetailSalesJob"
9. Job starts → Step "csvToDbStep" starts
10. ┌─ CHUNK 1 ────────────────────────────────────────────┐
    │ Reader reads CSV rows 1-10 → 10 RetailSalesCsvRow    │
    │ Processor transforms each → 10 RetailSalesRecord     │
    │ Writer inserts 10 records into retail_sales → COMMIT  │
    └──────────────────────────────────────────────────────┘
11. ... repeats for chunks 2 through 100 ...
12. Step completes (1000 rows read, 1000 written)
13. Job completes with status: COMPLETED
14. App stays running (Tomcat serving H2 console on port 8080)
```

**Console output you'll see:**

```
Processing Transaction ID: 1
Transformed → TxnID=1, priceWithTax=59.0, discounted=135.0, doubled=300.0...
Processing Transaction ID: 2
...
Step: [csvToDbStep] executed in 215ms
Job: [SimpleJob: [name=importRetailSalesJob]] completed with status: [COMPLETED]
```

---

## 7. Key Terms Glossary

| Term                  | Meaning                                                                                                                  |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| **Spring Boot**       | A framework that makes it easy to create stand-alone Spring applications with minimal configuration                      |
| **Spring Batch**      | A framework for batch processing — reading, transforming, and writing large volumes of data                              |
| **Job**               | The entire batch operation (e.g., "import sales data")                                                                   |
| **Step**              | One phase of a job (e.g., "read CSV and write to DB")                                                                    |
| **Chunk**             | A group of records processed together in one transaction                                                                 |
| **ItemReader**        | Reads data one item at a time from a source                                                                              |
| **ItemProcessor**     | Transforms each item (input → output)                                                                                    |
| **ItemWriter**        | Writes a chunk of items to a destination                                                                                 |
| **JobRepository**     | Stores metadata about job executions                                                                                     |
| **DTO**               | Data Transfer Object — a simple class that holds data                                                                    |
| **Entity**            | A JPA-annotated class that maps to a database table                                                                      |
| **JPA**               | Java Persistence API — standard for ORM in Java                                                                          |
| **Hibernate**         | The most popular JPA implementation                                                                                      |
| **ORM**               | Object-Relational Mapping — maps Java objects to database rows                                                           |
| **H2**                | A lightweight, in-memory SQL database                                                                                    |
| **Maven**             | A build tool that manages dependencies and compiles Java                                                                 |
| **Lombok**            | A library that generates boilerplate code via annotations                                                                |
| **Bean**              | An object managed by the Spring container                                                                                |
| **`@Configuration`**  | Marks a class as a source of bean definitions                                                                            |
| **`@Component`**      | Marks a class for auto-detection by Spring's component scan                                                              |
| **`@Bean`**           | Marks a method whose return value becomes a Spring bean                                                                  |
| **`@Entity`**         | Marks a class as a JPA entity (maps to a DB table)                                                                       |
| **`@Id`**             | Marks a field as the primary key                                                                                         |
| **`@Data`**           | Lombok: generates getters, setters, toString, equals, hashCode                                                           |
| **`@Slf4j`**          | Lombok: creates a logger field                                                                                           |
| **Classpath**         | The set of directories/JARs where Java looks for classes and resources. `src/main/resources/` files are on the classpath |
| **DDL**               | Data Definition Language — SQL commands like CREATE TABLE, ALTER TABLE                                                   |
| **`ddl-auto=update`** | Hibernate auto-creates/modifies tables to match entity classes                                                           |
