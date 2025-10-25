# 💰 Personal P2P Payment Service (PPPS)

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)
[![Kafka](https://img.shields.io/badge/Kafka-3.9-black?logo=apachekafka)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **Secure, Event-Driven, and Atomic Peer-to-Peer Payments Platform with Real-time Notifications**

A production-ready Spring Boot application enabling instant fund transfers between users with guaranteed financial integrity through ACID-compliant transactions, double-entry bookkeeping, and asynchronous event processing via Apache Kafka.

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
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

### 💸 **Core Functionality**
- ✅ **Instant P2P Transfers**: Send money using receiver's phone number or username
- ✅ **Bank Withdrawals**: Withdraw funds to external Nigerian bank accounts
- ✅ **Secure PIN Authentication**: Bcrypt-hashed PIN for transaction authorization
- ✅ **Real-time Balance Queries**: Check wallet balance instantly
- ✅ **Transaction History**: Paginated, filterable transaction logs with search
- ✅ **Funding System**: Multiple payment gateway support (Paystack, Flutterwave)

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

## 🏗️ Architecture

### **Event-Driven Transfer Flow**

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
│ 1. Lock Sender Wallet       │◄───── SELECT FOR UPDATE
│ 2. Verify Balance           │
│ 3. Verify PIN               │
│ 4. Lock Receiver Wallet     │
│ 5. Debit Sender             │
│ 6. Credit Receiver          │
│ 7. Create Transaction       │
│ 8. Log to Ledger (2x)       │
│ 9. Commit Transaction       │
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
│     └─ Send SMS (Twilio/Termii)                 │
│     └─ Send Email (SendGrid/MailGun)            │
│                                                  │
│  📊 AnalyticsService                             │
│     └─ Store Events in Analytics DB              │
│     └─ Generate Reports & Insights               │
│                                                  │
│  🔍 AuditService                                 │
│     └─ Compliance Logging                        │
│     └─ Fraud Detection                           │
└──────────────────────────────────────────────────┘
```

### **System Architecture Diagram**

```
┌──────────────────────────────────────────────────────────────┐
│                      API Gateway Layer                       │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │   Auth     │  │  Transfer  │  │ Withdrawal │            │
│  │ Controller │  │ Controller │  │ Controller │            │
│  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘            │
└─────────┼────────────────┼────────────────┼──────────────────┘
          │                │                │
          ▼                ▼                ▼
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
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │  Transfer  │  │ Withdrawal │  │   Wallet   │            │
│  │  Service   │  │  Service   │  │  Service   │            │
│  └──────┬─────┘  └──────┬─────┘  └──────┬─────┘            │
└─────────┼────────────────┼────────────────┼──────────────────┘
          │                │                │
          │   @Transactional (ACID)         │
          ▼                ▼                ▼
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
| **Language** | Java | 21 | Core application language |
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

- **Java 21+** ([Download](https://adoptium.net/))
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

The application will start on **http://localhost:8080**

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
      - "8080:8080"
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
http://localhost:8080/api/v1
```

### **Authentication Endpoints**

#### 1. Register New User
```http
POST /api/v1/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "phoneNumber": "+2349000000001",
  "pin": "123456",
  "fullName": "John Doe"
}
```

**Response:**
```json
{
  "userId": "uuid-here",
  "username": "johndoe",
  "email": "john@example.com",
  "phoneNumber": "+2349000000001",
  "walletId": "wallet-uuid-here",
  "balance": 0.00,
  "message": "User registered successfully"
}
```

#### 2. Login (supports username/email/phone/userId)
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "identifier": "johndoe",  // or email, phone, userId
  "pin": "123456"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "uuid-here",
  "username": "johndoe",
  "email": "john@example.com",
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
  "receiverPhoneNumber": "+2349000000002",
  "amount": 5000.00,
  "securePin": "123456",
  "narration": "Payment for services"
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
  "amount": 5000.00,
  "status": "SUCCESS",
  "completedAt": "2025-10-22T10:30:00Z"
}
```

---

### **Withdrawal Endpoints** 🔐 *Requires JWT*

#### 4. Withdraw to Bank Account
```http
POST /api/v1/withdrawals
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "amount": 10000.00,
  "bankName": "Access Bank",
  "accountNumber": "0123456789",
  "accountName": "John Doe",
  "securePin": "123456",
  "narration": "Withdrawal to bank"
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Withdrawal initiated successfully",
  "transactionId": "uuid-here",
  "estimatedTime": "5-10 minutes"
}
```

---

### **Wallet Endpoints** 🔐 *Requires JWT*

#### 5. Check Balance
```http
GET /api/v1/balance/{walletId}
Authorization: Bearer {jwt_token}
```

**Response:**
```json
{
  "walletId": "uuid-here",
  "balance": 15000.00,
  "currency": "NGN"
}
```

#### 6. Get Transaction History
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
      "amount": 5000.00,
      "status": "SUCCESS",
      "initiatedAt": "2025-10-22T00:00:00Z"
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
http://localhost:8080/swagger-ui.html
```

---

## 🗄️ Database Schema

### **Entity Relationship Diagram**

```
┌─────────────────┐       ┌─────────────────┐
│      User       │       │     Wallet      │
├─────────────────┤       ├─────────────────┤
│ userId (PK)     │──────▶│ id (PK)         │
│ username        │   1:1 │ userId (FK)     │
│ email           │       │ balance         │
│ phoneNumber     │       │ currency        │
│ hashedPin       │       │ version         │
│ wallet_id (FK)  │       └─────────────────┘
└─────────────────┘               │
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

## 📨 Kafka Topics & Events

### **Topic Configuration**

| Topic Name | Partitions | Retention | Purpose |
|------------|-----------|-----------|---------|
| `transactions.completed` | 3 | 7 days | P2P transfer events |
| `withdrawal.completed` | 3 | 7 days | Bank withdrawal events |
| `user.notifications` | 5 | 1 day | SMS/Email notifications |

### **Event Schemas**

#### **TransactionCompletedEvent**
```json
{
  "transactionId": "UUID",
  "senderWalletId": "UUID",
  "receiverWalletId": "UUID",
  "amount": "BigDecimal",
  "status": "SUCCESS|FAILED|PENDING",
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

1. User registers with username/email/phone and PIN
2. PIN is hashed using Bcrypt (cost factor: 10)
3. User logs in with any identifier (username/email/phone/userId)
4. JWT token issued (expires in 24 hours)
5. All protected endpoints require `Authorization: Bearer {token}`

### **Security Features**

- ✅ **JWT Stateless Authentication**
- ✅ **Bcrypt Password Hashing**
- ✅ **Multi-factor Identifier Login** (username/email/phone/userId)
- ✅ **CSRF Protection Disabled** (API-only, token-based auth)
- ✅ **Rate Limiting** (100 requests/minute per IP)
- ✅ **SQL Injection Protection** (JPA Parameterized Queries)
- ✅ **XSS Protection** (JSON responses only)
- ✅ **Event Integrity** (Kafka publish only after DB commit)

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
curl http://localhost:8080/actuator/health
```

### **Prometheus Metrics**
```bash
curl http://localhost:8080/actuator/prometheus
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
