# Azentio Hackathon 2026

A transaction ingestion and compliance-monitoring application built with **Next.js**, **Spring Boot**, **PostgreSQL**, and **Apache ActiveMQ**.

The project currently supports CSV-based transaction ingestion, validation, asynchronous processing through ActiveMQ, transaction persistence, and ingestion error logging.

---

## Project Structure

```text
Azentio-Hackathon-2026/
│
├── frontend/
│   └── Next.js application
│
├── hackathon-backend/
│   └── Spring Boot application
│
├── docker-compose.yml
│   └── PostgreSQL 17
│
└── README.md
```

---

## Architecture

```text
                    ┌─────────────────────┐
                    │      Frontend       │
                    │      Next.js        │
                    │    localhost:3000   │
                    └──────────┬──────────┘
                               │
                               │ HTTP
                               ▼
                    ┌─────────────────────┐
                    │       Backend       │
                    │     Spring Boot     │
                    │    localhost:8080   │
                    └───────┬─────┬───────┘
                            │     │
                JDBC        │     │ JMS
                            │     │
                            ▼     ▼
                    ┌──────────┐ ┌──────────────┐
                    │PostgreSQL│ │ Apache       │
                    │   :5432  │ │ ActiveMQ     │
                    │          │ │   :61616     │
                    └──────────┘ └──────────────┘
```

### Transaction ingestion flow

```text
CSV Upload
    │
    ▼
Next.js Frontend
    │
    ▼
POST /api/v1/ingest/csv
    │
    ▼
Spring Boot Backend
    │
    ├── CSV validation
    │
    ├── Transaction message creation
    │
    ▼
Apache ActiveMQ
    │
    ▼
Transaction Consumer
    │
    ├── Deserialize message
    ├── Validate account
    ├── Validate currency
    ├── Validate channel
    └── Validate jurisdiction
    │
    ├───────────────┐
    │               │
  Valid           Invalid
    │               │
    ▼               ▼
transactions   ingestion_error_logs
```

---

# Prerequisites

Install the following before running the project:

- **Java 21**
- **Maven**
- **Node.js**
- **npm**
- **Docker Desktop**
- **Apache ActiveMQ Classic**

You can verify the main tools with:

```bash
java -version
mvn -version
node -v
npm -v
docker --version
docker compose version
```

---

# 1. Clone the Repository

```bash
git clone <repository-url>
cd Azentio-Hackathon-2026
```

---

# 2. Start PostgreSQL

The project includes a Docker Compose configuration for PostgreSQL 17.

From the project root:

```bash
docker compose up -d postgres
```

Check that the container is running:

```bash
docker ps
```

You should see:

```text
nextjs_backend_postgres
```

### PostgreSQL Configuration

| Property | Value |
|---|---|
| Database | `nextjs_app` |
| Username | `postgres` |
| Password | `postgres` |
| Host | `localhost` |
| Port | `5432` |

The Docker Compose configuration is:

```yaml
services:
  postgres:
    image: postgres:17
    container_name: nextjs_backend_postgres
    restart: unless-stopped

    environment:
      POSTGRES_DB: nextjs_app
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres

    ports:
      - "5432:5432"

    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

---

# 3. Start Apache ActiveMQ

The Spring Boot backend expects an ActiveMQ broker at:

```text
tcp://localhost:61616
```

with:

```text
Username: admin
Password: admin
```

Make sure ActiveMQ is running before starting the backend.

The application currently uses the queue:

```text
transaction.ingestion
```

### ActiveMQ Connection

```text
Broker URL: tcp://localhost:61616
Username: admin
Password: admin
```

If ActiveMQ is not running, the backend will not be able to publish/consume transaction messages.

---

# 4. Start the Spring Boot Backend

Open a terminal:

```bash
cd hackathon-backend
```

Run the application using Maven:

```bash
mvn spring-boot:run
```

Alternatively, on Windows:

```bash
mvnw.cmd spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

### Backend Configuration

The current `application.properties` contains:

```properties
spring.application.name=hackathon-backend

spring.datasource.url=jdbc:postgresql://localhost:5432/nextjs_app
spring.datasource.username=postgres
spring.datasource.password=postgres

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Kolkata

spring.activemq.broker-url=tcp://localhost:61616
spring.activemq.user=admin
spring.activemq.password=admin
```

Spring Boot automatically creates/updates the JPA tables because:

```properties
spring.jpa.hibernate.ddl-auto=update
```

---

# 5. Start the Next.js Frontend

Open another terminal:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

The frontend will normally be available at:

```text
http://localhost:3000
```

Open the URL in your browser.

---

# 6. Access the Application

Once all services are running:

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend | http://localhost:8080 |
| PostgreSQL | localhost:5432 |
| ActiveMQ Broker | localhost:61616 |

The main application should be accessed through:

```text
http://localhost:3000
```

---

# 7. Upload a Transaction CSV

The frontend provides a CSV upload interface.

The backend endpoint is:

```http
POST /api/v1/ingest/csv
```

The request uses:

```text
multipart/form-data
```

with the field:

```text
file
```

Example CSV:

```csv
sourceAccountId,amount,currency,counterpartyName,counterpartyAccount,channel,jurisdiction,timestamp
ACC_000001,50000,INR,ABC Traders,ACC_000002,UPI,IN,2026-09-19T10:30:00
ACC_000002,125000,USD,XYZ Corp,ACC_000001,WIRE,US,2026-09-19T11:00:00
ACC_000001,70000,INR,John Doe,ACC_000002,SWIFT,IN,2026-09-19T11:30:00
```

The frontend sends this file to:

```text
http://localhost:8080/api/v1/ingest/csv
```

---

# 8. CSV Validation

The CSV ingestion layer validates basic record structure before publishing the message to ActiveMQ.

Examples of rejected records:

- Missing source account ID
- Missing amount
- Invalid amount format
- Missing currency
- Missing counterparty name
- Missing counterparty account
- Missing channel
- Missing jurisdiction
- Invalid timestamp format

Malformed records are reported in the API response.

Example:

```json
{
  "totalRecords": 3,
  "successfulRecords": 2,
  "failedRecords": 1,
  "errors": [
    {
      "row": 2,
      "error": "Invalid amount"
    }
  ]
}
```

---

# 9. Business Validation

After a valid CSV record is published to ActiveMQ, the transaction is processed asynchronously.

The backend validates:

### Source Account

The source account must exist in the `accounts` table.

Example:

```text
ACC_000001 -> valid
ACC_000002 -> valid
ACC_999999 -> invalid
```

### Currency

Currency must contain exactly three uppercase letters.

```text
INR -> valid
USD -> valid
EUR -> valid
```

### Channel

Channel must not be empty.

### Jurisdiction

Jurisdiction must contain exactly two uppercase letters.

```text
IN -> valid
US -> valid
```

---

# 10. Ingestion Error Logging

Invalid business-level transaction records are stored in:

```text
ingestion_error_logs
```

The table records:

- Entity type
- Raw transaction message
- Error message
- Timestamp

For transaction ingestion:

```text
entityType = TRANSACTION
```

Example error:

```text
Account not found: ACC_999999
```

This prevents invalid messages from being repeatedly treated as successful transactions.

---

# 11. Database Tables

The application currently uses the following tables:

```text
customers
accounts
transactions
alerts
compliance_cases
ingestion_error_logs
```

The main transaction relationship is:

```text
customers
    │
    ▼
accounts
    │
    ▼
transactions
    │
    ├── alerts
    │
    └── compliance_cases
```

---

# 12. Verify PostgreSQL

You can connect to PostgreSQL using:

```bash
psql -h localhost -p 5432 -U postgres -d nextjs_app
```

Password:

```text
postgres
```

List tables:

```sql
\dt
```

Check accounts:

```sql
SELECT *
FROM accounts;
```

Check transactions:

```sql
SELECT *
FROM transactions
ORDER BY id DESC;
```

Check ingestion errors:

```sql
SELECT *
FROM ingestion_error_logs
ORDER BY timestamp DESC;
```

---

# 13. Stop the Application

Stop the Next.js development server:

```text
Ctrl + C
```

Stop the Spring Boot application:

```text
Ctrl + C
```

Stop PostgreSQL:

```bash
docker compose down
```

### Important

`docker compose down` removes the PostgreSQL container but does **not** remove the named Docker volume.

The data is stored in:

```text
postgres_data
```

Therefore, the database data is preserved when the container is stopped and recreated.

To remove the database volume as well:

```bash
docker compose down -v
```

**Warning:** this deletes the PostgreSQL data stored in the Docker volume.

---

# 14. Troubleshooting

## PostgreSQL connection refused

Check:

```bash
docker ps
```

Make sure:

```text
nextjs_backend_postgres
```

is running and port `5432` is available.

---

## ActiveMQ connection refused

Make sure ActiveMQ is running and listening on:

```text
localhost:61616
```

The configured credentials are:

```text
admin / admin
```

---

## Frontend cannot call the backend

The frontend runs on:

```text
http://localhost:3000
```

while the backend runs on:

```text
http://localhost:8080
```

The backend must allow requests from the frontend through CORS.

The frontend API request is:

```text
http://localhost:8080/api/v1/ingest/csv
```

---

## Port already in use

Check which process is using a port.

On Windows:

```powershell
netstat -ano | findstr :5432
```

For the backend:

```powershell
netstat -ano | findstr :8080
```

For the frontend:

```powershell
netstat -ano | findstr :3000
```

---

# 15. Development Workflow

For normal development, start the services in this order:

```text
1. PostgreSQL
       ↓
2. ActiveMQ
       ↓
3. Spring Boot Backend
       ↓
4. Next.js Frontend
```

Commands:

### Terminal 1 — PostgreSQL

```bash
docker compose up -d postgres
```

### Terminal 2 — Backend

```bash
cd hackathon-backend
mvn spring-boot:run
```

### Terminal 3 — Frontend

```bash
cd frontend
npm install
npm run dev
```

Then open:

```text
http://localhost:3000
```

---

# 16. Current Features

- CSV transaction upload
- Drag-and-drop CSV upload UI
- CSV parsing
- Required field validation
- Data type validation
- ActiveMQ-based asynchronous transaction processing
- JSON transaction messaging
- Source account validation
- Currency validation
- Channel validation
- Jurisdiction validation
- Transaction persistence
- Ingestion error logging
- PostgreSQL persistence
- Dockerized PostgreSQL

---

# 17. Current Limitations / Future Work

The following features can be added as the project evolves:

- Transaction listing/dashboard
- Transaction search and filtering
- Alert generation
- Compliance case creation
- Risk scoring
- AML rule engine
- Transaction monitoring
- Ingestion job/status tracking
- Dead-letter queue handling
- Retry policies
- Authentication and authorization
- Production environment configuration
- Environment variables/secrets instead of development credentials
- Docker Compose support for the complete application stack
- Production deployment

---

# 18. Quick Start

For someone setting up the project for the first time:

```bash
# Clone
git clone <repository-url>

# Enter project
cd Azentio-Hackathon-2026

# Start PostgreSQL
docker compose up -d postgres

# Start ActiveMQ separately
# Make sure localhost:61616 is available

# Start backend
cd hackathon-backend
mvn spring-boot:run

# In another terminal
cd ../frontend
npm install
npm run dev
```

Then open:

```text
http://localhost:3000
```

---

## Service Access Summary

```text
Frontend
http://localhost:3000

Backend
http://localhost:8080

CSV Ingestion API
http://localhost:8080/api/v1/ingest/csv

PostgreSQL
localhost:5432

ActiveMQ
localhost:61616
```

---

## Technology Stack

### Frontend

- Next.js
- React
- TypeScript
- Tailwind CSS

### Backend

- Java
- Spring Boot
- Spring Data JPA
- Spring JMS
- Apache ActiveMQ
- Jackson
- Maven

### Database

- PostgreSQL 17
- Hibernate / JPA

### Infrastructure

- Docker
- Docker Compose
- Apache ActiveMQ
