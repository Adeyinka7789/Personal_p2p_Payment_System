# Personal_p2p_Payment_System
Personal P2P Payment Service (PPPS)
💰 Secure, Auditable, and Atomic Peer-to-Peer Payments Platform
The Personal P2P Payment Service (PPPS) is a robust back-end application built with Spring Boot, designed to facilitate secure and instant fund transfers between registered users. With a primary focus on financial integrity, PPPS leverages database-level transaction management, row-level locking, and a double-entry accounting ledger to ensure ACID compliance for every transaction.
Project Overview

Version: 1.0
Objective: Create a secure, instant, and auditable platform for users to transfer funds directly to one another, prioritizing transactional integrity.
Core Goal: Guarantee financial atomicity, ensuring debit and credit operations occur simultaneously with rollback capabilities if either fails.

Key Features



Feature
Description
Technical Implementation



Atomic P2P Transfer
Instant transfer of funds between users using the receiver's phone number and sender's secure PIN.
Encapsulated within @Transactional with SELECT FOR UPDATE for row-level locking.


Financial Integrity
Ensures funds are never lost or duplicated, treating all debit/credit operations as a single atomic unit.
PostgreSQL transactions and JPA concurrency controls (@Version).


Auditability
Provides an immutable record of all financial movements.
Double-entry accounting using Wallet and LedgerEntry entities.


Security
Protects user accounts and transaction requests.
Spring Security, JWT for stateless authentication, and Bcrypt for PIN hashing.


Rate Limiting
Prevents abuse and ensures system stability.
Custom filter integrated into the Spring Security chain, leveraging Redis for counts.


Technical Stack

Backend Framework: Java 21, Spring Boot 3+
API Design: RESTful API (application/json)
Database: PostgreSQL (Primary persistence layer for transactional data)
Caching/Messaging: Redis (Used for caching session data, rate limiting counts, or potentially message queuing)
Security: Spring Security, JWT, Bcrypt
Build Tool: Maven

Financial Integrity: The Atomic Transfer Flow
🚀 The core value of PPPS lies in its ability to execute transfers with guaranteed Atomicity, Consistency, Isolation, and Durability (ACID). This is achieved through a precise sequence within the TransferService:

Row Locking: Locks the sender's Wallet using SELECT FOR UPDATE to prevent concurrent overdrafts.
Overdraft Check: Validates Sender Balance >= Amount.
Debit/Credit: Updates the balances of the sender and receiver wallets.
Ledger Logging: Creates two immutable LedgerEntry records (Debit and Credit) for auditable history.
Commit/Rollback: Commits the transaction if all steps succeed (status: SUCCESS). Rolls back to the starting state if any step fails (e.g., connection error, overdraft).

Getting Started
Prerequisites

Docker
Docker Compose
Java 21+
Maven

Installation

Environment Setup

Clone the repository:git clone https://github.com/your-username/ppps.git
cd ppps


Configure environment variables in .env:POSTGRES_DB=ppps_db
POSTGRES_USER=ppps_user
POSTGRES_PASSWORD=local_test_password


Start PostgreSQL and Redis services:docker-compose up -d


Verify services are running:docker ps

Ensure ppps-redis-1 and ppps-ppps-db-1 are active.


Spring Boot Application

Ensure database and Redis properties in src/main/resources/application.properties match the Docker configuration:spring.datasource.url=jdbc:postgresql://localhost:5432/ppps_db
spring.datasource.username=ppps_user
spring.datasource.password=local_test_password
spring.redis.host=localhost
spring.redis.port=6379


Build and run the application:mvn clean package
java -jar target/ppps-0.0.1-SNAPSHOT.jar





API Endpoints (Quick Reference)



Category
Method
Path
Description
Authentication



Authentication
POST
/api/v1/register
Create a new user and provision a wallet.
None


Authentication
POST
/api/v1/auth/login
Authenticate and receive a JWT.
None


Transfer
POST
/api/v1/transfers
Initiate a P2P fund transfer (requires PIN).
JWT Required


Balance
GET
/api/v1/balance/{walletId}
Check the current wallet balance.
JWT Required


Funding
POST
/api/v1/funding/deposit
Test endpoint to deposit funds into a wallet.
JWT Required



Test connectivity with http://localhost:8080/actuator/health.

Architecture Diagram
View the PPPS Architecture Diagram for a detailed visualization of the atomic transfer flow.
Data Modeling (PostgreSQL)

Wallet Entity: id (UUID, PK), userId (UUID, FK to User), balance (BigDecimal, ≥ 0), currency (String, default NGN), version (Long for optimistic locking).
Transaction Entity: id (UUID, PK), senderWalletId (UUID, FK to Wallet), receiverWalletId (UUID, FK to Wallet), amount (BigDecimal), status (Enum: PENDING, SUCCESS, FAILED), initiatedAt (Instant).
LedgerEntry Entity: id (UUID, PK), transactionId (UUID, FK to Transaction), walletId (UUID, FK to Wallet), entryType (Enum: DEBIT/CREDIT), amount (BigDecimal), createdAt (Instant).

Contributing
🤝 We welcome contributions to enhance PPPS! Please follow these steps:

Fork the repository.
Create a feature branch (git checkout -b feature-name).
Commit changes (git commit -m "Add feature-name").
Push to the branch (git push origin feature-name).
Open a pull request, ensuring all contributions maintain financial integrity and ACID compliance.

License
This project is licensed under the MIT License - see the LICENSE file for details.
Acknowledgments

Inspired by the need for secure P2P payments in local markets.
Developed with guidance from the TDAD and PRD documents.
