
# 💰 Personal P2P Payment Service (PPPS)

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)
[![Kafka](https://img.shields.io/badge/Kafka-3.9-black?logo=apachekafka)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **Secure, Event-Driven P2P Payments with Escrow Protection & Real-time Notifications**

A production-ready Spring Boot application enabling instant fund transfers between users with **escrow protection for large amounts**, guaranteed financial integrity through ACID-compliant transactions, double-entry bookkeeping, and asynchronous event processing via Apache Kafka.

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Escrow System](#-escrow-system)
- [Event-Driven Design](#-event-driven-design)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running with Docker](#running-with-docker)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Kafka Topics & Events](#-kafka-topics--events)
- [Security](#-security)
- [Testing](#-testing)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

### 🔐 **Financial Integrity**
- **ACID-Compliant Transactions**: Every transfer is atomic - either fully succeeds or completely rolls back
- **Row-Level Locking**: PostgreSQL `SELECT FOR UPDATE` prevents concurrent overdrafts
- **Optimistic Locking**: JPA `@Version` for conflict detection
- **Double-Entry Bookkeeping**: Immutable ledger entries for complete audit trail

### 🛡️ **Escrow Protection System**
- **Large Amount Protection**: Transfers ≥₦50,000 automatically go to escrow
- **30-Minute Cancellation Window**: Senders can cancel transactions within 30 minutes
- **Fee-Only Deduction**: Only transaction fee deducted immediately, principal held
- **Auto-Completion**: System automatically completes escrow after 30 minutes if not cancelled
- **Real-time Countdown**: Frontend shows remaining cancellation time
- **Full Refund**: Both principal and fee refunded on cancellation

### 💸 **Core Functionality**
- ✅ **Instant P2P Transfers**: Send money using receiver's phone number
- ✅ **Escrow Transfers**: Large amounts protected with cancellation window
- ✅ **Wallet Funding**: Deposit via Paystack, Flutterwave (Card, Bank Transfer, USSD)
- ✅ **Bank Withdrawals**: Withdraw funds to any Nigerian bank account
- ✅ **Secure PIN Authentication**: Bcrypt-hashed PIN for transaction authorization
- ✅ **Real-time Balance Queries**: Check wallet balance instantly
- ✅ **Transaction History**: Paginated, filterable transaction logs with search
- ✅ **Multi-Gateway Support**: Seamless integration with multiple payment providers

### 🚀 **Event-Driven Architecture**
- 📡 **Asynchronous Notifications**: SMS and email alerts via Kafka events
- 📊 **Real-time Analytics**: Transaction tracking and business intelligence
- 🔄 **Scalable Processing**: Decoupled services for high throughput
- 🎯 **Event Sourcing**: Complete audit trail of all financial activities

### 🛡️ **Security & Performance**
- 🔒 **JWT Authentication**: Stateless authentication with secure token validation
- 🚦 **Rate Limiting**: IP-based request throttling (100 req/min per IP)
- ⚡ **Redis Caching**: Fast session management and rate limit counters
- 📊 **Prometheus Metrics**: Production-ready monitoring and observability
- 🎫 **Kafka Event Streaming**: Reliable message delivery with guaranteed ordering

---

## 🛡️ Escrow System

### **Smart Escrow Protection**

The system automatically protects large transfers (≥₦50,000) with a 30-minute escrow period:

#### **Escrow Flow**
```
┌──────────────┐
│   Sender     │
└──────┬───────┘
       │ Send ₦60,000+
       ▼
┌─────────────────────┐
│  Transfer Service   │
├─────────────────────┤
│ 1. Detect Amount    │───► ≥₦50,000 → ESCROW
│ 2. Deduct Fee Only  │───► ₦950 fee deducted
│ 3. Hold Principal   │───► ₦60,000 held in escrow
│ 4. Set PENDING      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Transaction       │
│   Status: PENDING   │
│   (30-min window)   │
└──────────┬──────────┘
           │
    ┌──────┴──────┐
    ▼             ▼
┌─────────┐   ┌─────────┐
│ Cancel  │   │ Auto-   │
│ Within  │   │ Complete│
│ 30 mins │   │ After   │
└─────────┘   │ 30 mins │
    │         └─────────┘
    ▼               ▼
┌─────────┐     ┌─────────┐
│ Full    │     │ Principal│
│ Refund  │     │ Transfer │
│ (Fee +  │     │ to       │
│ Principal)│   │ Receiver │
└─────────┘     └─────────┘
```

#### **Escrow Scenarios**

**Scenario 1: User Cancels Within 30 Minutes**
```java
// User clicks "Cancel" button in dashboard
escrowService.cancelEscrowTransaction(transactionId, senderWalletId);

// Result:
// - Status: PENDING → CANCELLED
// - Balance: +₦60,000 (principal returned) + ₦950 (fee refunded)
// - Receiver notified: "Transaction cancelled by sender"
```

**Scenario 2: Auto-Completion After 30 Minutes**
```java
// System automatically completes after timeout
@Scheduled(fixedRate = 60000) // Runs every minute
public void autoCompletePendingTransactions() {
    // Find transactions older than 30 minutes
    completeEscrowTransaction(transaction);
    
    // Result:
    // - Status: PENDING → SUCCESS  
    // - Balance: Sender -₦60,000, Receiver +₦60,000
    // - Both parties notified
}
```

#### **Frontend Escrow Features**
- ⏰ **Real-time Countdown Timer**: "Cancel within 25m 30s"
- 🔴 **Cancel Button**: Only shown for PENDING escrow transactions
- 📱 **Mobile Responsive**: Works on all devices
- 🔄 **Auto-Refresh**: Updates status and timers automatically

---

## 💰 Complete Money Flow

### **1. Deposit Flow (Money In)**

```
┌──────────────┐
│     User     │
│  (Mobile/Web)│
└──────┬───────┘
       │ 1. POST /api/v1/funding
       │    {amount: 10000, walletId: "..."}
       ▼
┌─────────────────────┐
│  Funding Controller │
└──────────┬──────────┘
           │ 2. Generate payment reference
           ▼
┌─────────────────────┐
│  Paystack/Flutterwave│
│  Payment Gateway     │◄──── 3. User pays via Card/Bank/USSD
└──────────┬──────────┘
           │ 4. Webhook: payment.success
           ▼
┌─────────────────────┐
│  Webhook Handler    │
└──────────┬──────────┘
           │ 5. Verify signature
           │ 6. Credit wallet (+₦10,000)
           ▼
┌─────────────────────┐
│  Database (Wallet)  │
│  Balance: 0 → 10000 │
└──────────┬──────────┘
           │ 7. Publish event
           ▼
┌─────────────────────┐
│  Kafka Topic:       │
│  deposit.completed  │
└──────────┬──────────┘
           │
           ├─────► 📧 Send SMS: "₦10,000 credited"
           └─────► 📊 Analytics: Track deposit
```

### **2. P2P Transfer Flow (Money Movement)**

#### **Instant Transfer (<₦50,000)**
```
┌──────────────┐                           ┌──────────────┐
│  Sender Wallet│                           │Receiver Wallet│
│  (₦10,000)   │                           │  (₦5,000)    │
└──────┬───────┘                           └──────────────┘
       │ 1. POST /api/v1/transfers
       │    {amount: 3000, receiver: "234..."}
       ▼
┌─────────────────────┐
│ Transfer Service    │
│ @Transactional      │
├─────────────────────┤
│ 2. Lock both wallets│
│ 3. Verify PIN       │
│ 4. Check balance    │
│ 5. Debit sender     │──► Sender: ₦10,000 - ₦3,000 = ₦7,000
│ 6. Credit receiver  │──► Receiver: ₦5,000 + ₦3,000 = ₦8,000
│ 7. Create ledger (2x)│
│ 8. COMMIT           │
└──────────┬──────────┘
           │ 9. afterCommit() → Kafka
           ▼
┌─────────────────────┐
│  transactions.      │
│  completed (Topic)  │
└──────────┬──────────┘
           │
           ├─────► 📧 SMS to both parties
           ├─────► 📊 Analytics tracking
           └─────► 🔍 Fraud detection check
```

#### **Escrow Transfer (≥₦50,000)**
```
┌──────────────┐                           ┌──────────────┐
│  Sender Wallet│                           │Receiver Wallet│
│  (₦100,000)  │                           │  (₦20,000)   │
└──────┬───────┘                           └──────────────┘
       │ 1. POST /api/v1/transfers
       │    {amount: 60000, receiver: "234..."}
       ▼
┌─────────────────────┐
│ Transfer Service    │
│ @Transactional      │
├─────────────────────┤
│ 2. Detect Escrow    │───► Amount ≥₦50,000 → ESCROW
│ 3. Lock wallets     │
│ 4. Verify PIN       │
│ 5. Deduct Fee Only  │──► Sender: ₦100,000 - ₦950 = ₦99,050
│ 6. Hold Principal   │──► ₦60,000 held (not transferred)
│ 7. Set PENDING      │
│ 8. COMMIT           │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  30-Minute Window   │
│  ┌─────────────────┐│
│  │  Cancel Button  ││
│  │  ⏰ 29:45 left  ││
│  └─────────────────┘│
└──────────┬──────────┘
           │
    ┌──────┴──────┐
    ▼             ▼
┌─────────┐   ┌─────────┐
│ User    │   │ Timeout │
│ Cancels │   │ (30min) │
└─────────┘   └─────────┘
    │             │
    ▼             ▼
┌─────────┐   ┌─────────┐
│ Refund  │   │ Complete│
│ +₦60,950│   │ Transfer│
│ CANCELLED│   │ SUCCESS │
└─────────┘   └─────────┘
```

### **3. Withdrawal Flow (Money Out)**

```
┌──────────────┐
│  User Wallet │
│  (₦7,000)    │
└──────┬───────┘
       │ 1. POST /api/v1/withdrawals
       │    {amount: 2500, bank: "First National", account: "001..."}
       ▼
┌─────────────────────┐
│ Withdrawal Service  │
│ @Transactional      │
├─────────────────────┤
│ 2. Verify PIN       │
│ 3. Lock wallet      │
│ 4. Check balance    │
│ 5. Debit wallet     │──► Wallet: ₦7,000 - ₦2,500 - ₦50 (fee) = ₦4,450
│ 6. Debit fee        │──► Platform fee: +₦50
│ 7. COMMIT to DB     │
└──────────┬──────────┘
           │ 8. Call external bank API
           ▼
┌─────────────────────┐
│ Paystack Transfer   │
│ API or Bank API     │──► 9. Send ₦2,500 to bank account
└──────────┬──────────┘
           │ 10. Success/Failed
           ▼
┌─────────────────────┐
│ Update Transaction  │
│ Status in DB        │
└──────────┬──────────┘
           │ 11. If SUCCESS → Kafka event
           ▼           If FAILED → Reverse debit
┌─────────────────────┐
│  withdrawal.        │
│  completed (Topic)  │
└──────────┬──────────┘
           │
           ├─────► 📧 SMS: "₦2,500 sent to bank"
           ├─────► 📊 Analytics: Withdrawal volume
           └─────► 🔍 Audit: Compliance logging
```

---

## 🏗️ Architecture

### **Enhanced Escrow Architecture**

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ POST /api/v1/transfers
       ▼
┌─────────────────────────────┐
│   TransferController        │
│  (JWT Authentication)       │
└──────────┬──────────────────┘
           │
           ▼
┌─────────────────────────────┐
│    TransferService          │
│  @Transactional             │
├─────────────────────────────┤
│ 1. Check Amount             │───► ≥₦50,000 → Escrow Flow
│ 2. Lock Wallets             │
│ 3. Verify Balance           │
│ 4. Verify PIN               │
│ 5. Process Based on Type:   │
│    • Instant: Full transfer │
│    • Escrow: Fee only       │
│ 6. Create Transaction       │
│ 7. Log to Ledger            │
│ 8. Commit Transaction       │
└──────────┬──────────────────┘
           │
           │ afterCommit()
           ▼
┌─────────────────────────────┐
│   Kafka Producer            │
│  TransactionCompletedEvent  │
└──────────┬──────────────────┘
           │
           │ Publish to Topic
           ▼
┌─────────────────────────────────────────────────┐
│            Apache Kafka Cluster                 │
│  Topic: transactions.completed                  │
│  Topic: withdrawal.completed                    │
└──────────┬──────────────────────────────────────┘
           │
           │ Subscribe
           ▼
┌──────────────────────────────────────────────────┐
│         Kafka Consumer Services                  │
├──────────────────────────────────────────────────┤
│  📧 NotificationService                          │
│  📊 AnalyticsService                             │
│  🔍 AuditService                                 │
│  ⏰ EscrowService (Scheduled)                    │───► Auto-completes escrow
└──────────────────────────────────────────────────┘
```

### **Escrow Service Components**

```java
@Service
public class EscrowService {
    
    // Check if transfer requires escrow
    public boolean requiresEscrow(BigDecimal amount) {
        return amount.compareTo(new BigDecimal("50000.00")) >= 0;
    }
    
    // Auto-complete pending transactions every minute
    @Scheduled(fixedRate = 60000)
    public void autoCompletePendingTransactions() {
        // Complete transactions older than 30 minutes
    }
    
    // Cancel escrow transaction
    public void cancelEscrowTransaction(UUID transactionId, UUID senderWalletId) {
        // Validate ownership and time window
        // Update status to CANCELLED
        // Refund principal + fee
    }
}
```

### **System Architecture Diagram**

```
                    External Payment Gateways
┌─────────────────────────────────────────────────────────────┐
│  🏦 Paystack  │  🏦 Flutterwave  │  🏦 Bank APIs           │
│  (Card/Bank)  │  (Card/Bank)     │  (Direct Transfer)      │
└────────┬──────┴─────────┬────────┴──────────┬──────────────┘
         │                │                    │
         │ Webhook/API    │ Webhook/API        │ Callback
         ▼                ▼                    ▼
┌──────────────────────────────────────────────────────────────┐
│                      API Gateway Layer                       │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────┐│
│  │   Auth     │  │  Transfer  │  │ Withdrawal │  │Funding ││
│  │ Controller │  │ Controller │  │ Controller │  │Control.││
│  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘  └───┬────┘│
└─────────┼────────────────┼────────────────┼─────────────┼────┘
          │                │                │             │
          ▼                ▼                ▼             ▼
┌──────────────────────────────────────────────────────────────┐
│                   Security Filter Chain                       │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │    JWT     │  │ Rate Limit │  │   CORS     │            │
│  │   Filter   │  │   Filter   │  │   Filter   │            │
│  └────────────┘  └────────────┘  └────────────┘            │
└──────────────────────────────────────────────────────────────┘
          │
          ▼
┌──────────────────────────────────────────────────────────────┐
│                     Service Layer                             │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────┐│
│  │  Transfer  │  │ Withdrawal │  │   Wallet   │  │Funding ││
│  │  Service   │  │  Service   │  │  Service   │  │Service ││
│  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘  └───┬────┘│
└─────────┼────────────────┼────────────────┼─────────────┼────┘
          │                │                │             │
          │   @Transactional (ACID)         │             │
          ▼                ▼                ▼             ▼
┌──────────────────────────────────────────────────────────────┐
│                  Repository Layer (JPA)                       │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │  Wallet    │  │Transaction │  │   Ledger   │            │
│  │ Repository │  │ Repository │  │ Repository │            │
│  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘            │
└─────────┼────────────────┼────────────────┼──────────────────┘
          │                │                │
          ▼                ▼                ▼
┌──────────────────────────────────────────────────────────────┐
│                   PostgreSQL Database                         │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │  wallets   │  │transactions│  │  ledgers   │            │
│  │   users    │  │   events   │  │   logs     │            │
│  └────────────┘  └────────────┘  └────────────┘            │
└──────────────────────────────────────────────────────────────┘

          ┌─────────────────────────────────┐
          │    Redis Cache (Rate Limits)    │
          └─────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                      Apache Kafka                             │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Topic: transactions.completed                         │  │
│  │  Topic: withdrawal.completed                           │  │
│  │  Topic: deposit.completed                              │  │
│  │  Topic: user.notifications                             │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────┬───────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────┐
│                   Consumer Services                           │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │Notification│  │ Analytics  │  │   Audit    │            │
│  │  Service   │  │  Service   │  │  Service   │            │
│  └────────────┘  └────────────┘  └────────────┘            │
└──────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Language** | Java | 17 | Core application language |
| **Framework** | Spring Boot | 3.5.6 | Application framework |
| **Security** | Spring Security + JWT | 6.x | Authentication & authorization |
| **Database** | PostgreSQL | 15+ | Primary data store |
| **Cache** | Redis | 7+ | Session management & rate limiting |
| **Message Broker** | Apache Kafka | 3.9 | Event streaming & async processing |
| **ORM** | Spring Data JPA (Hibernate) | 6.6.x | Database access layer |
| **Build Tool** | Maven | 3.9+ | Dependency management |
| **Container** | Docker + Docker Compose | Latest | Service orchestration |
| **Monitoring** | Micrometer + Prometheus | Latest | Metrics & observability |
| **Documentation** | SpringDoc OpenAPI | 2.6.0 | API documentation |

---

## 📡 Event-Driven Design

### **Event Flow Architecture**

The application uses **Apache Kafka** for asynchronous, event-driven communication between services:

#### **Transaction Lifecycle Events**

1. **Transaction Initiated** → Database transaction starts
2. **Transaction Committed** → Event published to Kafka
3. **Event Consumed** → Multiple downstream services process independently:
   - 📧 **Notification Service**: Sends SMS/Email to users
   - 📊 **Analytics Service**: Records metrics and generates reports
   - 🔍 **Audit Service**: Logs compliance data

#### **Benefits of Event-Driven Design**

✅ **Decoupling**: Services are independent and loosely coupled
✅ **Scalability**: Consumer services can scale horizontally
✅ **Resilience**: Kafka guarantees message delivery even if consumers are down
✅ **Auditing**: Complete event history stored in Kafka topics
✅ **Real-time**: Notifications and analytics happen in near real-time

---

## 🚀 Getting Started

### Prerequisites

Ensure you have the following installed:

- **Java 17+** ([Download](https://adoptium.net/))
- **Maven 3.9+** ([Download](https://maven.apache.org/download.cgi))
- **Docker & Docker Compose** ([Download](https://www.docker.com/get-started))
- **Git** ([Download](https://git-scm.com/downloads))

### Installation

#### 1️⃣ **Clone the Repository**

```bash
git clone https://github.com/your-username/ppps.git
cd ppps
```

#### 2️⃣ **Configure Environment**

Create a `.env` file in the project root:

```env
# Database Configuration
POSTGRES_DB=ppps_db
POSTGRES_USER=ppps_user
POSTGRES_PASSWORD=local_test_password

# Redis Configuration
REDIS_PASSWORD=

# Kafka Configuration
KAFKA_BROKERS=localhost:9094
KAFKA_AUTO_CREATE_TOPICS=true

# Application Configuration
JWT_SECRET=your-secret-key-change-in-production
CONVERSION_MARGIN=0.005
PLATFORM_WALLET_ID=00000000-0000-0000-0000-000000000001
```

#### 3️⃣ **Start Infrastructure Services**

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Kafka (port 9094)
- Zookeeper (port 2181)

Verify services are running:
```bash
docker ps
```

You should see:
- `ppps-postgres`
- `ppps-redis`
- `ppps-kafka`
- `ppps-zookeeper`

#### 4️⃣ **Build the Application**

```bash
mvn clean package -DskipTests
```

#### 5️⃣ **Run the Application**

```bash
java -jar target/ppps-0.0.1-SNAPSHOT.jar
```

Or with Maven:
```bash
mvn spring-boot:run
```

The application will start on **http://localhost:8081**

---

### Running with Docker Compose

**Complete Stack (Recommended)**

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: ppps-postgres
    environment:
      POSTGRES_DB: ppps_db
      POSTGRES_USER: ppps_user
      POSTGRES_PASSWORD: local_test_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: ppps-redis
    ports:
      - "6379:6379"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: ppps-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: ppps-kafka
    depends_on:
      - zookeeper
    ports:
      - "9094:9094"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9094
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

  app:
    build: .
    container_name: ppps-app
    depends_on:
      - postgres
      - redis
      - kafka
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ppps_db
      SPRING_DATASOURCE_USERNAME: ppps_user
      SPRING_DATASOURCE_PASSWORD: local_test_password
      SPRING_REDIS_HOST: redis
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9094

volumes:
  postgres_data:
```

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

---

## 📚 API Documentation

### Base URL
```
http://localhost:8081/api/v1
```

### **Authentication Endpoints**

#### 1. Register New User
```http
POST /api/v1/register
Content-Type: application/json

{
  "phoneNumber": "2348012345678",
  "pin": "1234"
}
```

**Response:**
```json
{
  "userId": "uuid-here",
  "phoneNumber": "2348012345678",
  "walletId": "wallet-uuid-here",
  "balance": 0.00,
  "message": "User registered successfully"
}
```

#### 2. Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "phoneNumber": "+2347030834157",
  "pin": "7789"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "uuid-here",
  "phoneNumber": "+2347030834157",
  "expiresIn": 86400000
}
```

---

### **Transfer Endpoints** 🔐 *Requires JWT*

#### 3. Execute P2P Transfer
```http
POST /api/v1/transfers
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "receiverPhoneNumber": "2347030834157",
  "amount": 500.50,
  "securePin": "7789",
  "narration": "Dinner reimbursement"
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Transfer completed successfully",
  "transactionId": "uuid-here"
}
```

**Kafka Event Published:**
```json
{
  "transactionId": "uuid-here",
  "senderWalletId": "sender-wallet-uuid",
  "receiverWalletId": "receiver-wallet-uuid",
  "amount": 500.50,
  "status": "SUCCESS",
  "completedAt": "2025-10-27T10:30:00Z"
}
```

---

### **Escrow Endpoints** 🔐 *Requires JWT*

#### 4. Cancel Escrow Transaction
```http
POST /api/v1/transfers/{transactionId}/cancel
Authorization: Bearer {jwt_token}
```

**Response:**
```json
{
  "status": "success",
  "message": "Transfer cancelled successfully"
}
```

**Cancellation Flow:**
1. User clicks cancel button in dashboard
2. System validates transaction is within 30-minute window
3. Updates transaction status to CANCELLED
4. Refunds principal amount + fee to sender
5. Notifies receiver about cancellation
6. Updates frontend UI in real-time

---

### **Funding Endpoints** 🔐 *Requires JWT*

#### 5. Fund Wallet (via Payment Gateway)
```http
POST /api/v1/funding
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "walletId": "c2df2774-04dd-4fd2-9d84-232ee9097dea",
  "amount": 150000.50
}
```

**Response:**
```json
{
  "status": "success",
  "message": "✅ Deposit successful for wallet: c2df2774-04dd-4fd2-9d84-232ee9097dea",
  "authorizationUrl": "https://paystack.com/pay/xyz123",
  "reference": "TXN_REF_12345"
}
```

**Payment Flow:**
1. User initiates funding via API
2. System generates payment reference
3. User redirected to Paystack/Flutterwave payment page
4. User completes payment (Card/Bank Transfer/USSD)
5. Gateway sends webhook notification to your backend
6. System credits wallet and publishes `deposit.completed` event
7. User receives SMS/Email confirmation

---

### **Withdrawal Endpoints** 🔐 *Requires JWT*

#### 6. Withdraw to Bank Account
```http
POST /api/v1/withdrawals
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "amount": 2500.00,
  "accountNumber": "0012345678",
  "bankName": "First National Bank",
  "securePin": "7789"
}
```

**Response:**
```json
{
  "transactionRef": "TXN-1761601094077",
  "amount": 2500.00,
  "accountNumber": "0012345678",
  "bankName": "First National Bank",
  "status": "PENDING",
  "createdAt": "2025-10-27T21:38:14.077445900Z",
  "message": "Withdrawal request accepted. Funds transfer is now processing asynchronously."
}
```

**Withdrawal Flow:**
1. User submits withdrawal request with PIN
2. System validates PIN and sufficient balance
3. Debits user wallet immediately
4. Calls external bank API (via Paystack/Flutterwave Transfer API)
5. If successful: Publishes `withdrawal.completed` event
6. If failed: Reverses debit and updates status
7. User receives notification with outcome

---

### **Wallet Endpoints** 🔐 *Requires JWT*

#### 7. Check Balance
```http
GET /api/v1/balance
Authorization: Bearer {jwt_token}
```

**Response:**
```json
{
  "balance": {
    "walletId": "c2df2774-04dd-4fd2-9d84-232ee9097dea",
    "amount": 299442.99,
    "currency": "NGN"
  },
  "status": "success"
}
```

#### 8. Get Transaction History
```http
GET /api/v1/transactions/{walletId}?pageNumber=0&pageSize=10
Authorization: Bearer {jwt_token}
```

**Response:**
```json
{
  "transactions": [
    {
      "transactionId": "uuid-here",
      "type": "TRANSFER",
      "senderWalletId": "uuid",
      "receiverWalletId": "uuid",
      "amount": 500.50,
      "status": "SUCCESS",
      "initiatedAt": "2025-10-27T00:00:00Z"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalPages": 5
}
```

---

### **Interactive API Documentation**

Access Swagger UI at:
```
http://localhost:8081/swagger-ui.html
```

---

## 🗄️ Database Schema

### **Entity Relationship Diagram**

```
┌─────────────────┐       ┌─────────────────┐
│      User       │       │     Wallet      │
├─────────────────┤       ├─────────────────┤
│ userId (PK)     │──────▶│ id (PK)         │
│ phoneNumber     │   1:1 │ userId (FK)     │
│ hashedPin       │       │ balance         │
│ wallet_id (FK)  │       │ currency        │
│ createdAt       │       │ version         │
└─────────────────┘       └─────────────────┘
                                  │
                                  │ 1:M
                                  ▼
                          ┌─────────────────┐
                          │  Transaction    │
                          ├─────────────────┤
                          │ id (PK)         │
                          │ senderWalletId  │
                          │ receiverWalletId│
                          │ amount          │
                          │ status          │
                          │ initiatedAt     │
                          └─────────────────┘
                                  │
                                  │ 1:2
                                  ▼
                          ┌─────────────────┐
                          │  LedgerEntry    │
                          ├─────────────────┤
                          │ id (PK)         │
                          │ transactionId   │
                          │ walletId        │
                          │ entryType       │
                          │ amount          │
                          │ createdAt       │
                          └─────────────────┘
```

---

## 🏦 Payment Gateway Integration

### **Supported Gateways**

#### **1. Paystack**
- **Features**: Card payments, Bank Transfer, USSD, Mobile Money
- **Webhooks**: `charge.success`, `transfer.success`, `transfer.failed`
- **API Endpoints**:
  - Funding: `/transaction/initialize`
  - Withdrawal: `/transfer`
  - Verification: `/transaction/verify/:reference`

#### **2. Flutterwave**
- **Features**: Card payments, Bank Transfer, USSD, Mobile Money
- **Webhooks**: `charge.completed`, `transfer.completed`
- **API Endpoints**:
  - Funding: `/payments`
  - Withdrawal: `/transfers`
  - Verification: `/transactions/:id/verify`

### **Gateway Configuration**

```yaml
# application.yml
payment:
  gateways:
    paystack:
      secret-key: ${PAYSTACK_SECRET_KEY}
      public-key: ${PAYSTACK_PUBLIC_KEY}
      webhook-url: ${APP_URL}/api/v1/webhooks/paystack
    
    flutterwave:
      secret-key: ${FLUTTERWAVE_SECRET_KEY}
      public-key: ${FLUTTERWAVE_PUBLIC_KEY}
      webhook-url: ${APP_URL}/api/v1/webhooks/flutterwave
```

### **Webhook Security**

All incoming webhooks are verified using HMAC-SHA512 signatures:

```java
// Verify Paystack webhook
String signature = request.getHeader("x-paystack-signature");
String computedHash = HmacUtils.hmacSha512Hex(secretKey, payload);
if (!signature.equals(computedHash)) {
    throw new SecurityException("Invalid webhook signature");
}
```

### **Supported Payment Methods**

| Method | Paystack | Flutterwave | Average Time |
|--------|----------|-------------|--------------|
| **Card** | ✅ | ✅ | Instant |
| **Bank Transfer** | ✅ | ✅ | 2-10 mins |
| **USSD** | ✅ | ✅ | 2-5 mins |
| **Mobile Money** | ❌ | ✅ | Instant |
| **QR Code** | ✅ | ✅ | Instant |

---

## 📨 Kafka Topics & Events

### **Topic Configuration**

| Topic Name | Partitions | Retention | Purpose |
|------------|-----------|-----------|---------|
| `transactions.completed` | 3 | 7 days | P2P transfer events |
| `withdrawal.completed` | 3 | 7 days | Bank withdrawal events |
| `deposit.completed` | 3 | 7 days | Wallet funding events |
| `user.notifications` | 5 | 1 day | SMS/Email notifications |

### **Event Schemas**

#### **TransactionCompletedEvent**
```json
{
  "transactionId": "UUID",
  "senderWalletId": "UUID",
  "receiverWalletId": "UUID",
  "amount": "BigDecimal",
  "status": "SUCCESS|FAILED|PENDING|CANCELLED",
  "completedAt": "ISO 8601 Timestamp"
}
```

#### **WithdrawalCompletedEvent**
```json
{
  "transactionId": "UUID",
  "senderWalletId": "UUID",
  "amount": "BigDecimal",
  "bankName": "String",
  "accountNumber": "String (masked)",
  "status": "SUCCESS|FAILED|PENDING",
  "completedAt": "ISO 8601 Timestamp"
}
```

### **Consumer Services**

#### **1. Notification Service**
- **Group ID**: `notification-service`
- **Purpose**: Sends real-time SMS and email notifications
- **Integration**: Twilio, Termii, SendGrid

#### **2. Analytics Service**
- **Group ID**: `analytics-service`
- **Purpose**: Tracks metrics, generates reports
- **Features**: Transaction volume, success rates, user behavior

#### **3. Audit Service** (Future)
- **Group ID**: `audit-service`
- **Purpose**: Compliance logging, fraud detection
- **Features**: Regulatory reporting, anomaly detection

---

## 🔒 Security

### **Authentication Flow**

1. User registers with phone number and PIN
2. PIN is hashed using Bcrypt (cost factor: 10)
3. User logs in with phone number
4. JWT token issued (expires in 24 hours)
5. All protected endpoints require `Authorization: Bearer {token}`

### **Security Features**

- ✅ **JWT Stateless Authentication**
- ✅ **Bcrypt Password Hashing**
- ✅ **Phone-based Authentication**
- ✅ **CSRF Protection Disabled** (API-only, token-based auth)
- ✅ **Rate Limiting** (100 requests/minute per IP)
- ✅ **SQL Injection Protection** (JPA Parameterized Queries)
- ✅ **XSS Protection** (JSON responses only)
- ✅ **Event Integrity** (Kafka publish only after DB commit)
- ✅ **Escrow Protection** (Large amount safety with cancellation window)

### **Environment Variables (Production)**

```env
# JWT
JWT_SECRET=change-this-to-a-very-long-random-secret-minimum-256-bits
JWT_EXPIRATION=86400000

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/ppps_db
SPRING_DATASOURCE_USERNAME=ppps_prod_user
SPRING_DATASOURCE_PASSWORD=very-secure-password-here

# Redis
SPRING_REDIS_HOST=prod-redis
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=redis-secure-password

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=prod-kafka:9092
SPRING_KAFKA_PRODUCER_ACKS=all
SPRING_KAFKA_PRODUCER_RETRIES=3

# Escrow Configuration
ESCROW_THRESHOLD=50000.00
ESCROW_TIMEOUT_MINUTES=30
```

---

## 🧪 Testing

### **Run Unit Tests**

```bash
mvn test
```

### **Run Integration Tests**

```bash
mvn verify
```

### **Test Coverage Report**

```bash
mvn clean test jacoco:report
```

View report at: `target/site/jacoco/index.html`

### **Escrow Testing**

#### Test Escrow Scenarios
```bash
# Test escrow creation
curl -X POST http://localhost:8081/api/v1/transfers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"receiverPhoneNumber": "2347030834157", "amount": 60000, "securePin": "7789", "narration": "Escrow test"}'

# Test cancellation
curl -X POST http://localhost:8081/api/v1/transfers/$TRANSACTION_ID/cancel \
  -H "Authorization: Bearer $TOKEN"
```

### **Kafka Testing**

#### Monitor Kafka Topics
```bash
# List topics
docker exec -it ppps-kafka kafka-topics --bootstrap-server localhost:9094 --list

# Consume messages from topic
docker exec -it ppps-kafka kafka-console-consumer \
  --bootstrap-server localhost:9094 \
  --topic transactions.completed \
  --from-beginning
```

#### Test Event Publishing
```bash
# Execute a transfer and watch Kafka logs
docker-compose logs -f app | grep "Kafka"
```

---

## 📊 Monitoring

### **Health Check**
```bash
curl http://localhost:8081/actuator/health
```

### **Prometheus Metrics**
```bash
curl http://localhost:8081/actuator/prometheus
```

### **Kafka Metrics**
- Consumer lag
- Message throughput
- Event processing time
- Failed message count

### **Application Metrics**
- Total transfers executed
- Transfer success/failure rate
- Average transfer duration
- Active user count
- Kafka events published/consumed
- Escrow transactions (created/completed/cancelled)

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **Commit** your changes: `git commit -m 'Add amazing feature'`
4. **Push** to the branch: `git push origin feature/amazing-feature`
5. **Open** a Pull Request

### **Code Standards**

- Follow Java code conventions
- Write unit tests for new features
- Maintain financial integrity in all transaction logic
- Document public APIs with JavaDoc
- Test Kafka event publishing/consuming
- Test escrow scenarios thoroughly

---

## 📝 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- Built with [Spring Boot](https://spring.io/projects/spring-boot)
- Event streaming powered by [Apache Kafka](https://kafka.apache.org/)
- Inspired by real-world P2P payment systems
- Developed with focus on financial integrity and security

---

## 📞 Support

For issues, questions, or contributions:

- **Issues**: [GitHub Issues](https://github.com/your-username/ppps/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-username/ppps/discussions)
- **Email**: Dotunm85@gmail.com
- **Phone**: +2347030834157

---

<div align="center">

**Made with ❤️ for secure, event-driven financial transactions**

⭐ **Star this repo if you find it useful!** ⭐

[View Demo](https://ppps-demo.example.com) • [Report Bug](https://github.com/your-username/ppps/issues) • [Request Feature](https://github.com/your-username/ppps/issues)

</div>
