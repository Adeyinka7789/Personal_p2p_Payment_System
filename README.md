💰 Personal P2P Payment Service (PPPS)

Secure, Auditable, and Atomic Peer-to-Peer Payments Platform

The Personal P2P Payment Service (PPPS) is a robust back-end application built with Spring Boot, designed to facilitate secure and instant fund transfers between registered users. Our primary engineering focus is financial integrity, leveraging database-level transaction management, row-level locking, and a double-entry accounting ledger to ensure strict ACID compliance for every transaction.

🌟 Project Overview

Detail

Value

Version

1.0

Objective

Create a secure, instant, and auditable platform for P2P transfers.

Core Goal

Guarantee financial atomicity; debit and credit must occur simultaneously or be fully rolled back.

Status

Complete / Ready for Deployment (adjust as needed)

Key Features

Feature

Description

Technical Implementation

Atomic P2P Transfer

Instant fund transfers requiring PIN verification.

Encapsulated within a @Transactional block with SELECT FOR UPDATE for row-level locking.

Financial Integrity

Guarantees funds are never lost or duplicated.

PostgreSQL transactions and JPA concurrency controls (@Version).

Auditability

Provides an immutable record of all financial movements.

Double-Entry Accounting Model using Wallet (mutable) and LedgerEntry (immutable).

Security

Protects user accounts and requests.

Spring Security, JWT for stateless authentication, and Bcrypt for PIN hashing.

Rate Limiting

Prevents abuse and ensures system stability.

Custom filter integrated into the Spring Security chain, leveraging Redis.

🛠️ Technical Stack

Backend Framework: Java 21, Spring Boot 3+

API Design: RESTful API (application/json)

Primary Database: PostgreSQL (Transactional persistence layer)

Caching & Concurrency: Redis (Used for rate limiting counts)

Security: Spring Security, JWT, Bcrypt

Build Tool: Maven

⚡ Financial Integrity: The Atomic Transfer Flow

The core value of PPPS is its guaranteed ACID compliance. This is achieved through a precise, sequential flow within the TransferService:

Row Locking: Locks the sender's Wallet using SELECT FOR UPDATE to prevent concurrent overdrafts.

Overdraft Check: Validates Sender Balance >= Amount.

Debit/Credit: Updates the balances of the sender and receiver wallets.

Ledger Logging: Creates two immutable LedgerEntry records (Debit and Credit) for auditable history.

Commit/Rollback: If all steps succeed, the transaction commits (SUCCESS). If any step fails (e.g., overdraft), the entire operation rolls back, guaranteeing consistency.

🐳 Getting Started (Dockerized Setup)

This project uses Docker Compose to manage the required PostgreSQL and Redis services, ensuring a consistent local development environment.

Prerequisites

You must have the following installed:

Docker and Docker Compose

Java 21+

Maven

1. Installation & Environment Setup

Clone the repository:

git clone [https://github.com/your-username/ppps.git](https://github.com/your-username/ppps.git)
cd ppps


Configure environment variables in the (assumed) .env file for the database:

# Example values for .env
POSTGRES_DB=ppps_db
POSTGRES_USER=ppps_user
POSTGRES_PASSWORD=local_test_password


Start PostgreSQL and Redis services using Docker Compose:

docker-compose up -d


Verify services are running: You should see containers for ppps-db and ppps-redis active.

2. Spring Boot Application

Verify application properties: Ensure the connection details in src/main/resources/application.properties (or application.yml) match your Docker Compose setup:

spring.datasource.url=jdbc:postgresql://localhost:5432/ppps_db
spring.datasource.username=ppps_user
spring.datasource.password=local_test_password
spring.redis.host=localhost
spring.redis.port=6379


Build and run the application:

mvn clean package
java -jar target/ppps-0.0.1-SNAPSHOT.jar


Test connectivity: Confirm the service is running successfully by hitting the actuator endpoint: http://localhost:8080/actuator/health.

🗺️ API Endpoints (Quick Reference)

Category

Method

Path

Description

Authentication

Authentication

POST

/api/v1/register

Creates a new user and provisions a wallet.

None

Authentication

POST

/api/v1/auth/login

Authenticates and returns a JWT.

None

Transfer

POST

/api/v1/transfers

Initiates a P2P fund transfer (requires PIN).

JWT Required

Balance

GET

/api/v1/balance/{walletId}

Checks the current wallet balance.

JWT Required

Funding

POST

/api/v1/funding/deposit

Test endpoint to deposit funds into a wallet.

JWT Required

🗃️ Data Modeling (PostgreSQL)

The financial integrity relies on three key entities:

Entity

Key Fields & Purpose

Concurrency Control

Wallet

id (PK), userId (FK), balance (mutable, ≥ 0), currency (NGN).

@Version for optimistic locking.

Transaction

id (PK, master ID), senderWalletId, receiverWalletId, amount, status (SUCCESS, FAILED).

Master reference for the entire operation.

LedgerEntry

id (PK), transactionId (FK), walletId, entryType (DEBIT/CREDIT).

Immutable audit log (Double-Entry).

🤝 Contributing

We welcome contributions to enhance PPPS! Please ensure any code changes strictly adhere to the project's ACID compliance standards.

Fork the repository.

Create a feature branch (git checkout -b feature-name).

Commit changes (git commit -m "Add feature-name").

Open a Pull Request.

License

This project is licensed under the MIT License - see the LICENSE file for details.
