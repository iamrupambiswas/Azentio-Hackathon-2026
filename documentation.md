# Transaction Ingestion Pipeline Documentation

## 1. Overview

This document captures the transaction ingestion flow implemented so far
in the Spring Boot hackathon backend.

The current pipeline supports:

-   CSV transaction ingestion
-   CSV record parsing and basic validation
-   Publishing valid records to Apache ActiveMQ
-   JSON serialization for JMS messages
-   Asynchronous transaction consumption
-   Business-level transaction validation
-   Account referential-integrity validation
-   Transaction persistence
-   Ingestion error logging for failed JMS/business validation
-   Preventing invalid JMS messages from being repeatedly redelivered

------------------------------------------------------------------------

## 2. High-Level Architecture

``` text
CSV Upload
    |
    v
CsvIngestionController
    |
    v
CsvIngestionService
    |
    |-- Parse CSV
    |-- Validate required fields
    |-- Validate data types
    |
    v
TransactionMessagePublisher
    |
    |-- Convert DTO -> TransactionIngestionMessage
    |-- Serialize object -> JSON
    |
    v
Apache ActiveMQ
Queue: transaction.ingestion
    |
    v
TransactionIngestionConsumer
    |
    |-- Deserialize JSON
    |-- Convert message -> TransactionIngestDto
    |
    v
IngestionService
    |
    v
TransactionValidationService
    |
    |-- Account exists?
    |-- Currency valid?
    |-- Channel valid?
    |-- Jurisdiction valid?
    |
    +-----------------------------+
    |                             |
   Valid                       Invalid
    |                             |
    v                             v
transactions              ingestion_error_logs
```

------------------------------------------------------------------------

# 3. Database Structure

The PostgreSQL database currently contains the following tables:

``` text
customers
accounts
transactions
alerts
compliance_cases
ingestion_error_logs
```

The relevant relationship for transaction ingestion is:

``` text
Customer
   |
   v
Account
   |
   v
Transaction
```

The `transactions.source_account_id` relationship is validated against
the existing `accounts` table through application-level validation.

------------------------------------------------------------------------

# 4. Transaction Input DTO

The transaction ingestion DTO represents the data received from CSV
ingestion.

``` java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionIngestDto {

    @NotNull(message = "Source account ID is required")
    private String sourceAccountId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Counterparty name is required")
    private String counterpartyName;

    @NotBlank(message = "Counterparty account is required")
    private String counterpartyAccount;

    @NotBlank(message = "Channel is required")
    private String channel;

    @NotBlank(message = "Jurisdiction is required")
    private String jurisdiction;

    private LocalDateTime timestamp;
}
```

The DTO contains both basic validation annotations and the fields
required for transaction persistence.

------------------------------------------------------------------------

# 5. CSV Ingestion

## Endpoint

The API exposes:

``` http
POST /api/v1/ingest/csv
```

The endpoint accepts a multipart CSV file:

``` java
@PostMapping(
    value = "/csv",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
public ResponseEntity<CsvImportResult> uploadCsv(
        @RequestParam("file") MultipartFile file) {

    CsvImportResult result =
            csvIngestionService.process(file);

    return ResponseEntity.ok(result);
}
```

------------------------------------------------------------------------

## CSV Processing

`CsvIngestionService` performs the following:

1.  Checks whether the file is empty.
2.  Reads the CSV using Apache Commons CSV.
3.  Reads the header row.
4.  Iterates through each record.
5.  Validates required fields.
6.  Converts the amount into `BigDecimal`.
7.  Converts the timestamp into `LocalDateTime`.
8.  Creates a `TransactionIngestDto`.
9.  Publishes the valid DTO to ActiveMQ.
10. Collects parsing errors without stopping the entire CSV import.

Example:

``` csv
sourceAccountId,amount,currency,counterpartyName,counterpartyAccount,channel,jurisdiction,timestamp
ACC_000001,50000,INR,ABC Traders,ACC_000002,UPI,IN,2026-09-19T10:30:00
ACC_000002,125000,USD,XYZ Corp,ACC_000001,WIRE,US,2026-09-19T11:00:00
ACC_000001,70000,INR,John Doe,ACC_000002,SWIFT,IN,2026-09-19T11:30:00
```

------------------------------------------------------------------------

# 6. CSV Result Model

The API returns:

``` java
public record CsvImportResult(
        int totalRecords,
        int successfulRecords,
        int failedRecords,
        List<CsvImportError> errors
) {}
```

Individual errors are represented as:

``` java
public record CsvImportError(
        long row,
        String error
) {}
```

Example successful response:

``` json
{
    "totalRecords": 3,
    "successfulRecords": 3,
    "failedRecords": 0,
    "errors": []
}
```

### Important Semantics

At the current stage:

``` text
successfulRecords
```

means:

> The CSV record was successfully parsed and published to ActiveMQ.

It does **not** yet mean that the transaction has successfully been
persisted in PostgreSQL.

This is because ActiveMQ processing happens asynchronously.

------------------------------------------------------------------------

# 7. ActiveMQ Configuration

The application uses Apache ActiveMQ with the queue:

``` text
transaction.ingestion
```

The queue is configured through:

``` java
public static final String TRANSACTION_QUEUE =
        "transaction.ingestion";
```

The application currently uses a single consumer during debugging:

``` java
factory.setConcurrency("1");
```

This can later be changed back to a higher concurrency configuration
such as:

``` java
factory.setConcurrency("3-10");
```

when concurrent processing is required.

------------------------------------------------------------------------

# 8. Transaction Message

The DTO is converted into a dedicated messaging object:

``` java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionIngestionMessage {

    private String sourceAccountId;
    private BigDecimal amount;
    private String currency;
    private String counterpartyName;
    private String counterpartyAccount;
    private String channel;
    private String jurisdiction;
    private LocalDateTime timestamp;
    private String source;
}
```

The `source` field allows the system to identify where the transaction
originated.

For CSV ingestion:

``` text
source = CSV
```

------------------------------------------------------------------------

# 9. JSON Messaging

Because the application is using Spring Boot 4 / Spring Framework 7 and
Jackson 3, the older Jackson 2 JMS message converter approach was not
used.

Instead, the application explicitly serializes the message into JSON:

``` java
String json = objectMapper.writeValueAsString(message);

jmsTemplate.convertAndSend(
        ActiveMqConfig.TRANSACTION_QUEUE,
        json
);
```

The ActiveMQ message therefore contains a JSON string.

Example:

``` json
{
    "sourceAccountId": "ACC_000001",
    "amount": 50000,
    "currency": "INR",
    "counterpartyName": "ABC Traders",
    "counterpartyAccount": "ACC_000002",
    "channel": "UPI",
    "jurisdiction": "IN",
    "timestamp": "2026-09-19T10:30:00",
    "source": "CSV"
}
```

------------------------------------------------------------------------

# 10. ActiveMQ Consumer

`TransactionIngestionConsumer` receives the JSON message:

``` java
@JmsListener(
    destination = "transaction.ingestion"
)
public void consume(String json) {
    ...
}
```

The JSON is deserialized using Jackson:

``` java
TransactionIngestionMessage message =
        objectMapper.readValue(
                json,
                TransactionIngestionMessage.class
        );
```

It is then converted back into:

``` text
TransactionIngestDto
```

and passed to:

``` java
ingestionService.ingestTransaction(dto);
```

------------------------------------------------------------------------

# 11. Business Validation

Business validation is handled separately by:

``` text
TransactionValidationService
```

The service currently validates:

### 11.1 Source Account

The source account must exist:

``` java
Account account = accountRepository
        .findById(dto.getSourceAccountId())
        .orElseThrow(() ->
                new IllegalArgumentException(
                        "Account not found: "
                                + dto.getSourceAccountId()
                ));
```

This provides application-level referential integrity.

Example:

``` text
ACC_000001 -> valid
ACC_000002 -> valid
ACC_999999 -> invalid
```

------------------------------------------------------------------------

### 11.2 Currency

Currency must be exactly three uppercase letters:

``` java
if (currency == null ||
        !currency.matches("[A-Z]{3}")) {

    throw new IllegalArgumentException(
            "Invalid currency: " + currency
    );
}
```

Examples:

``` text
INR -> valid
USD -> valid
EUR -> valid

inr -> invalid
IN -> invalid
INRR -> invalid
```

------------------------------------------------------------------------

### 11.3 Channel

Channel cannot be empty:

``` java
if (channel == null || channel.isBlank()) {
    throw new IllegalArgumentException(
            "Channel is required"
    );
}
```

------------------------------------------------------------------------

### 11.4 Jurisdiction

Jurisdiction must contain exactly two uppercase letters:

``` java
if (jurisdiction == null ||
        !jurisdiction.matches("[A-Z]{2}")) {

    throw new IllegalArgumentException(
            "Invalid jurisdiction: " + jurisdiction
    );
}
```

Examples:

``` text
IN -> valid
US -> valid

in -> invalid
IND -> invalid
```

------------------------------------------------------------------------

# 12. Ingestion Error Logging

An `IngestionErrorLog` entity has been created for failed ingestion
records.

``` java
@Entity
@Table(name = "ingestion_error_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String entityType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rawDataRecord;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }
}
```

For transaction ingestion:

``` text
entityType = TRANSACTION
```

The error log stores:

-   Entity type
-   Raw message data
-   Error message
-   Error timestamp

------------------------------------------------------------------------

# 13. Error Log Repository

The repository is:

``` java
public interface IngestionErrorLogRepository
        extends JpaRepository<IngestionErrorLog, String> {
}
```

This allows the application to persist ingestion failures into:

``` text
ingestion_error_logs
```

------------------------------------------------------------------------

# 14. Handling Invalid ActiveMQ Messages

Previously, the consumer did this:

``` java
catch (Exception e) {
    throw new RuntimeException(
            "Failed to process transaction message",
            e
    );
}
```

This caused invalid messages to be treated as listener failures.

For example:

``` text
Account not found: ACC003
```

would propagate out of the listener.

Depending on the JMS transaction/acknowledgement configuration, the
message could be redelivered repeatedly.

------------------------------------------------------------------------

## Current Approach

The consumer now catches the error and persists it:

``` java
catch (Exception e) {

    logIngestionError(
            json,
            e.getMessage()
    );
}
```

The error is persisted using:

``` java
private void logIngestionError(
        String rawData,
        String errorMessage) {

    IngestionErrorLog errorLog =
            IngestionErrorLog.builder()
                    .entityType("TRANSACTION")
                    .rawDataRecord(rawData)
                    .errorMessage(errorMessage)
                    .build();

    ingestionErrorLogRepository.save(errorLog);
}
```

The exception is not rethrown.

Therefore, under the current JMS listener configuration, normal listener
completion allows the invalid message to be consumed instead of
repeatedly failing the listener.

------------------------------------------------------------------------

# 15. Current Error Flow

For a transaction with an invalid account:

``` text
CSV
 |
 v
CSV Parsing
 |
 v
ActiveMQ
 |
 v
Consumer
 |
 v
TransactionValidationService
 |
 |-- Account not found
 |
 v
IngestionErrorLog
```

Example:

``` text
sourceAccountId = ACC_999999
```

produces:

``` text
Account not found: ACC_999999
```

and stores the original JSON message in:

``` text
ingestion_error_logs
```

------------------------------------------------------------------------

# 16. Malformed CSV vs Business Validation

There are currently two different validation stages.

## Stage 1: CSV Validation

This happens synchronously during the HTTP request.

Examples:

``` text
Missing sourceAccountId
Invalid amount
Missing currency
Invalid timestamp format
Missing counterparty
```

These errors are returned directly in:

``` json
{
    "totalRecords": 1,
    "successfulRecords": 0,
    "failedRecords": 1,
    "errors": [
        {
            "row": 1,
            "error": "..."
        }
    ]
}
```

The invalid record is not published to ActiveMQ.

------------------------------------------------------------------------

## Stage 2: Business Validation

This happens asynchronously after the record has been published.

Examples:

``` text
Account does not exist
Invalid currency
Missing channel
Invalid jurisdiction
```

These errors are currently persisted into:

``` text
ingestion_error_logs
```

------------------------------------------------------------------------

# 17. Complete Current Flow

``` text
                         HTTP
                          |
                          v
                 CSV Upload Endpoint
                          |
                          v
                 CsvIngestionService
                          |
                 +--------+--------+
                 |                 |
          malformed CSV          valid CSV
                 |                 |
                 v                 v
            API Error         ActiveMQ Queue
                                   |
                                   v
                      TransactionIngestionConsumer
                                   |
                          Deserialize JSON
                                   |
                                   v
                         IngestionService
                                   |
                                   v
                    TransactionValidationService
                                   |
                         +---------+---------+
                         |                   |
                       valid               invalid
                         |                   |
                         v                   v
                   transactions      ingestion_error_logs
```

------------------------------------------------------------------------

# 18. Verified Test Scenario

The database currently contains accounts such as:

``` text
ACC_000001
ACC_000002
```

A valid CSV using these IDs was successfully processed:

``` csv
sourceAccountId,amount,currency,counterpartyName,counterpartyAccount,channel,jurisdiction,timestamp
ACC_000001,50000,INR,ABC Traders,ACC_000002,UPI,IN,2026-09-19T10:30:00
ACC_000002,125000,USD,XYZ Corp,ACC_000001,WIRE,US,2026-09-19T11:00:00
ACC_000001,70000,INR,John Doe,ACC_000002,SWIFT,IN,2026-09-19T11:30:00
```

The CSV endpoint returned:

``` json
{
    "totalRecords": 3,
    "successfulRecords": 3,
    "failedRecords": 0,
    "errors": []
}
```

This confirmed that:

-   CSV parsing works.
-   DTO construction works.
-   JSON serialization works.
-   ActiveMQ publishing works.
-   ActiveMQ consumption works.
-   Jackson deserialization works.
-   Account validation works for existing accounts.
-   Transaction ingestion reaches the persistence layer.

------------------------------------------------------------------------

# 19. Example Invalid Test

To test referential integrity:

``` csv
sourceAccountId,amount,currency,counterpartyName,counterpartyAccount,channel,jurisdiction,timestamp
ACC_000001,50000,INR,ABC Traders,ACC_000002,UPI,IN,2026-09-19T10:30:00
ACC_999999,75000,INR,Unknown Corp,ACC_000001,WIRE,IN,2026-09-19T11:00:00
```

Expected result:

``` text
ACC_000001
    -> transactions

ACC_999999
    -> ingestion_error_logs
```

Expected error:

``` text
Account not found: ACC_999999
```

------------------------------------------------------------------------

# 20. Current Implementation Status

  -----------------------------------------------------------------------
  Requirement                         Status
  ----------------------------------- -----------------------------------
  CSV upload                          Done

  CSV parsing                         Done

  Required field validation           Done

  Amount parsing                      Done

  Timestamp parsing                   Done

  Malformed CSV rejection             Done

  ActiveMQ publishing                 Done

  JSON serialization                  Done

  ActiveMQ consumption                Done

  JSON deserialization                Done

  Account validation                  Done

  Currency validation                 Done

  Channel validation                  Done

  Jurisdiction validation             Done

  Transaction persistence             Done

  Ingestion error entity              Done

  Ingestion error repository          Done

  Business validation error logging   Done

  Avoid repeated processing of        Done
  expected invalid messages           

  Persist malformed CSV errors to DB  Pending

  Distinguish validation failures     Pending
  from unexpected system failures     

  Async ingestion job/status tracking Pending
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 21. Next Improvements

The next logical improvements are:

### 1. Persist CSV errors

Currently malformed CSV errors are returned through the API but are not
stored in:

``` text
ingestion_error_logs
```

We can make CSV and ActiveMQ ingestion errors consistent.

### 2. Separate expected validation errors from unexpected system errors

Currently:

``` java
catch (Exception e)
```

logs every exception.

A better design is:

``` text
Validation Exception
    -> log ingestion error
    -> consume message

Unexpected System Exception
    -> do not swallow
    -> allow retry / DLQ strategy
```

This prevents genuine infrastructure/database failures from being
silently marked as bad data.

### 3. Add error categories

For example:

``` text
MALFORMED_RECORD
REFERENTIAL_INTEGRITY
VALIDATION_ERROR
PERSISTENCE_ERROR
DESERIALIZATION_ERROR
```

### 4. Add ingestion status tracking

Because CSV `successfulRecords` currently means "published to ActiveMQ",
a future ingestion-job model could track:

``` text
RECEIVED
PUBLISHED
PROCESSING
PROCESSED
FAILED
```

This would provide accurate end-to-end ingestion status.

------------------------------------------------------------------------

# 22. Key Design Principle

The current architecture separates **data-format validation** from
**business validation**:

``` text
CSV Layer
    |
    | Format / required fields / type validation
    v
Message Queue
    |
    | Asynchronous processing
    v
Business Layer
    |
    | Referential integrity / domain validation
    v
Database
```

This separation makes the ingestion pipeline easier to extend and allows
the system to process large CSV imports asynchronously without making
the HTTP request wait for every transaction to reach the database.
