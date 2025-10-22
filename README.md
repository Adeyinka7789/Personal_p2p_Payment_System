# 💰 Personal P2P Payment Service (PPPS)

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **Secure, Auditable, and Atomic Peer-to-Peer Payments Platform**

A production-ready Spring Boot application enabling instant fund transfers between users with guaranteed financial integrity through ACID-compliant transactions and double-entry bookkeeping.

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running with Docker](#running-with-docker)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
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
- ✅ **Instant P2P Transfers**: Send money using receiver's phone number
- ✅ **Secure PIN Authentication**: Bcrypt-hashed PIN for transaction authorization
- ✅ **Real-time Balance Queries**: Check wallet balance instantly
- ✅ **Transaction History**: Paginated, filterable transaction logs with search
- ✅ **Funding System**: Test endpoint for wallet deposits

### 🛡️ **Security & Performance**
- 🔒 **JWT Authentication**: Stateless authentication with secure token validation
- 🚦 **Rate Limiting**: IP-based request throttling (100 req/min per IP)
- ⚡ **Redis Caching**: Fast session management and rate limit counters
- 📊 **Prometheus Metrics**: Production-ready monitoring and observability

---

## 🏗️ Architecture

### **Atomic Transfer Flow**

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
│ 9. Commit / Rollback        │
└──────────┬──────────────────┘
           │
           ▼
┌─────────────────────────────┐
│   PostgreSQL Database       │
│  - Wallets                  │
│  - Transactions             │
│  - Ledger Entries           │
│  - Users                    │
└─────────────────────────────┘
```

### **System Components**

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│              │     │              │     │              │
│  API Layer   │────▶│  Service     │────▶│  Repository  │
│  (REST)      │     │  Layer       │     │  (JPA)       │
│              │     │              │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
       │                    │                     │
       │                    │                     │
       ▼                    ▼                     ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Security     │     │ Transaction  │     │ PostgreSQL   │
│ Filter Chain │     │ Management   │     │ Database     │
└──────────────┘     └──────────────┘     └──────────────┘
       │                                          
       ▼                                          
┌──────────────┐                          ┌──────────────┐
│ Rate Limit   │                          │   Redis      │
│ Filter       │─────────────────────────▶│   Cache      │
└──────────────┘                          └──────────────┘
```

---

## 🛠️ Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 3.5.6 |
| **Security** | Spring Security + JWT | 6.x |
| **Database** | PostgreSQL | 15+ |
| **Cache** | Redis | 7+ |
| **ORM** | Spring Data JPA (Hibernate) | 6.6.x |
| **Build Tool** | Maven | 3.9+ |
| **Container** | Docker + Docker Compose | Latest |
| **Monitoring** | Micrometer + Prometheus | Latest |
| **Documentation** | SpringDoc OpenAPI | 2.6.0 |

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

# Application Configuration
JWT_SECRET=your-secret-key-change-in-production
CONVERSION_MARGIN=0.005
```

#### 3️⃣ **Start Infrastructure Services**

```bash
docker-compose up -d
```

Verify services are running:
```bash
docker ps
```

You should see:
- `ppps-postgres` (PostgreSQL on port 5432)
- `ppps-redis` (Redis on port 6379)

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

### Running with Docker

**Option 1: Docker Compose (Recommended)**

```bash
# Start all services (app + database + redis)
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

**Option 2: Manual Docker Build**

```bash
# Build the Docker image
docker build -t ppps:latest .

# Run the container
docker run -d \
  --name ppps-app \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/ppps_db \
  -e SPRING_DATASOURCE_USERNAME=ppps_user \
  -e SPRING_DATASOURCE_PASSWORD=local_test_password \
  ppps:latest
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
  "phoneNumber": "+2349000000001",
  "pin": "123456"
}
```

**Response:**
```json
{
  "userId": "uuid-here",
  "phoneNumber": "+2349000000001",
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
  "phoneNumber": "+2349000000001",
  "pin": "123456"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
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

---

### **Wallet Endpoints** 🔐 *Requires JWT*

#### 4. Check Balance
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

#### 5. Get Transaction History
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
│ phoneNumber     │   1:1 │ userId (FK)     │
│ hashedPin       │       │ balance         │
│ wallet_id (FK)  │       │ currency        │
└─────────────────┘       │ version         │
                          └─────────────────┘
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

### **Key Tables**

#### **users**
| Column | Type | Constraints |
|--------|------|-------------|
| user_id | VARCHAR(36) | PRIMARY KEY |
| phone_number | VARCHAR(20) | UNIQUE, NOT NULL |
| hashed_pin | VARCHAR(255) | NOT NULL |
| wallet_id | UUID | FOREIGN KEY |

#### **wallets**
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PRIMARY KEY |
| user_id | UUID | NOT NULL, INDEX |
| balance | DECIMAL(19,2) | NOT NULL, ≥ 0 |
| currency | VARCHAR(3) | DEFAULT 'NGN' |
| version | BIGINT | OPTIMISTIC LOCK |

#### **transactions**
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PRIMARY KEY |
| sender_wallet_id | UUID | FOREIGN KEY |
| receiver_wallet_id | UUID | FOREIGN KEY |
| amount | DECIMAL(19,2) | NOT NULL |
| status | ENUM | PENDING/SUCCESS/FAILED |
| initiated_at | TIMESTAMP | NOT NULL |

#### **ledger_entries**
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PRIMARY KEY |
| transaction_id | UUID | FOREIGN KEY |
| wallet_id | UUID | FOREIGN KEY |
| entry_type | ENUM | DEBIT/CREDIT |
| amount | DECIMAL(19,2) | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |

---

## 🔒 Security

### **Authentication Flow**

1. User registers with phone number and PIN
2. PIN is hashed using Bcrypt (cost factor: 10)
3. User logs in to receive JWT token
4. JWT contains user ID and expiration (24 hours)
5. All protected endpoints require `Authorization: Bearer {token}` header

### **Security Features**

- ✅ **JWT Stateless Authentication**
- ✅ **Bcrypt Password Hashing**
- ✅ **CSRF Protection Disabled** (API-only, token-based auth)
- ✅ **Rate Limiting** (100 requests/minute per IP)
- ✅ **SQL Injection Protection** (JPA Parameterized Queries)
- ✅ **XSS Protection** (JSON responses only)

### **Environment Variables (Production)**

```env
# Use strong secrets in production!
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

### **Manual Testing with cURL**

**1. Register User**
```bash
curl -X POST http://localhost:8080/api/v1/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+2349000000001",
    "pin": "123456"
  }'
```

**2. Login**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+2349000000001",
    "pin": "123456"
  }'
```

**3. Transfer Money**
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "receiverPhoneNumber": "+2349000000002",
    "amount": 1000.00,
    "securePin": "123456",
    "narration": "Test transfer"
  }'
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

### **Application Metrics**
- Total transfers executed
- Transfer success/failure rate
- Average transfer duration
- Active user count

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

---

## 📝 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- Built with [Spring Boot](https://spring.io/projects/spring-boot)
- Inspired by real-world P2P payment systems
- Developed with focus on financial integrity and security

---

## 📞 Support

For issues, questions, or contributions:

- **Issues**: [GitHub Issues](https://github.com/your-username/ppps/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-username/ppps/discussions)
- **Email**: Dotunm85@gmail.com
- - **Phone**: +2347030834157

---

<div align="center">

**Made with ❤️ for secure financial transactions**

⭐ **Star this repo if you find it useful!** ⭐

</div>
