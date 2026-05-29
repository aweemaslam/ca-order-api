# CA Orders API – Event Driven Order Management System

---

## 📌 Overview

This project is a **production-grade Spring Boot microservice** that implements a complete **Order Management System** with payment, fulfillment, inventory reservation, Kafka event streaming, Redis caching, and distributed scheduling.

The current implementation intentionally remains a modular monolith to keep operational complexity low while preserving clear service boundaries for future extraction into independent microservices.

The implementation goes beyond a standard CRUD service by incorporating **modern backend engineering practices**, including microservice-friendly architecture patterns such as:

* 🧾 Order Lifecycle Management (Create → Pay → Fulfill → Cancel)
* 🧠 Inventory Reservation using Redis + Lua scripting
* 📬 Outbox Pattern for reliable event publishing
* ⚡ Kafka-based event-driven communication
* 🔐 Schema-driven state transitions
* 🔁 Distributed scheduling with ShedLock
* 🧯 Resilience4j Circuit Breakers for external services
* 🐘 PostgreSQL + Liquibase for schema management
* 🚀 Dockerized full-stack environment
* Idempotent APIs

The system is designed to simulate a **real-world scalable e-commerce order workflow**.

---

## 🎯 Implemented User Stories

* ✅ As a customer, I can create an order with multiple products
* ✅ As a customer, I can retrieve an order by ID
* ✅ As a customer, I can pay for an order
* ✅ As a customer, I can transition order states
* ✅ As a system, inventory is reserved atomically before order creation
* ✅ As a system, stock is automatically released for cancelled orders
* ✅ As a system, order events are published asynchronously through Kafka
* ✅ As a system, stale pending orders are automatically cancelled

---

## 🏗️ Architecture

This project follows a hybrid architecture approach:

### 🔷 Hexagonal Architecture (Ports & Adapters)

* External/M2M integrations isolated via ports
* Feign adapters for Payment & Fulfillment services
* Decoupled business logic

### 🔷 N-Layered Architecture

* Controller → Service → Repository → Entity

### 🔷 Event-Driven Architecture

* Kafka for asynchronous communication
* Outbox Pattern for reliable event publishing

### 🔷 Key Design Principles

* SOLID principles
* Clean code practices
* Separation of concerns
* High scalability
* High resiliency
* High testability

---

## ⚙️ Tech Stack

* Java 21
* Spring Boot 3.5+
* Spring Data JPA
* PostgreSQL
* Redis (Inventory Cache)
* Apache Kafka
* Spring Cloud OpenFeign
* Resilience4j (Circuit Breaker)
* ShedLock
* Caffeine Cache
* Liquibase
* Docker & Docker Compose
* Swagger / OpenAPI
* Bean Validation
* Lombok
* Spring Bean Validation
* Outbox Pattern
* Schedulers
---

## 🚀 Features & Enhancements

### 📦 Order Management

* Create orders with multiple items
* Retrieve orders by ID
* Generic status transition support
* Idempotent order creation

---

### ⚡ Redis Inventory Reservation

* Product inventory cached in Redis
* Atomic stock reservation using Lua scripts
* Prevents overselling in concurrent scenarios

---

### 📡 Event-Driven Architecture

* Kafka-based asynchronous event publishing
* Outbox pattern implementation
* Reliable event delivery

Supported events:

* `ORDER_CREATED`
* `ORDER_STATUS_CHANGED`
* `STOCK_RELEASE_REQUESTED`

---

### 💳 Payment Integration

* OpenFeign payment gateway integration
* Retry mechanism
* Circuit breaker protection
* External service fallback handling

---

### 🚚 Fulfillment Integration

* Fulfillment provider integration
* Resilience4j protection
* Event-driven fulfillment workflow

---

### ⚡ Caching

* Caffeine cache for frequently accessed entities
* Reduces DB hits significantly

---

### ⏱️ Scheduler + ShedLock

* Outbox publisher scheduler
* Automatic pending order cancellation
* Distributed-safe execution using ShedLock

---

### ❗ Exception Handling

* Centralized exception handling via `@ControllerAdvice`

---

### 🧾 Validation

* Request validation using Bean Validation

---

## 🧩 Architecture Diagram

![Architecture Diagram](src/main/resources/images/architecture-diagram.png)


---

## 🔄 Order Flow Sequence Diagram

![order-creation-flow.png](src/main/resources/images/order-creation-flow.png)

---
## 🔄 Payment Flow Sequence Diagram

![payment-flow.png](src/main/resources/images/payment-flow.png)
---
## 🔄 Fulfillment Flow Sequence Diagram
![fulfillment-flow.png](src/main/resources/images/fulfillment-flow.png)
---
## 🔄 Outbox → kafka Publishing Sequence Diagram
![outbox-to-kafka-flow.png](src/main/resources/images/outbox-to-kafka-flow.png)
---
## 🔄 Complete Life Cycle Sequence Diagram
![full-flow.png](src/main/resources/images/full-flow.png)
---
## 🗄️ Database Strategy

The application uses:

* PostgreSQL for persistent storage
* Redis for inventory caching

### Initialization Strategy

Instead of querying products directly from the database repeatedly:

1. Products are loaded into Redis during startup
2. Inventory operations happen in Redis
3. Database stock updates occur asynchronously after payment

👉 Benefits:

* Faster inventory access
* Reduced database load
* Atomic reservation support
* Better scalability under high traffic

---

## 🔄 Scheduler

### 📡 Outbox Publisher Job

Publishes pending outbox events to Kafka.

```java
@Scheduled(fixedDelayString = "${app.jobs.outbox-publish-delay-ms:5000}")
public void publish() {
    // publish pending events
}
```

---

### ⏳ Pending Order Cancellation Job

Automatically cancels stale pending orders.

```java
@Scheduled(fixedDelayString = "${app.jobs.pending-order-cancellation-ms:5000}")
public void publish() {
    // cancel expired pending orders and restock products
}
```

---

## 📘 API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```
![swagger-ui.png](src/main/resources/images/swagger-ui.png)
---

## 📡 API Endpoints

### 1️⃣ Create Order

```http
POST /api/v1/orders
```

### Request Body

```json
{
  "customerEmail": "john@example.com",
  "currency": "EUR",
  "items": [
    {
      "productId": "uuid",
      "quantity": 2
    }
  ]
}
```

### Curl

```bash
curl -X POST 'http://localhost:8080/api/v1/orders' \
-H 'Content-Type: application/json' \
-H 'Idempotency-Key: abc-123' \
-d '{
  "customerEmail":"john@example.com",
  "currency":"EUR",
  "items":[
    {
      "productId":"uuid",
      "quantity":2
    }
  ]
}'
```

---

### 2️⃣ Get Order By ID

```http
GET /api/v1/orders/{id}
```

### Curl

```bash
curl -X GET 'http://localhost:8080/api/v1/orders/{id}'
```

---

### 3️⃣ Pay Order

```http
POST /api/v1/orders/{id}/pay
```

### Curl

```bash
curl -X POST 'http://localhost:8080/api/v1/orders/{id}/pay'
```

---

### 4️⃣ Transition Order Status

```http
POST /api/v1/orders/{id}/status/transition
```

### Request Body

```json
{
  "targetStatus": "FULFILLED"
}
```

---

## ▶️ Running the Application

### Prerequisites

* Java 21
* Docker
* Docker Compose

---

## 🐳 Docker

### Run Full Stack

```bash
docker-compose up --build
```

### Services Included

* PostgreSQL
* Redis
* Kafka
* Zookeeper
* WireMock Payment Service
* WireMock Fulfillment Service

---

## 🧪 Testing
Validated:
- business logic
- status transitions
- inventory reservation logic
- outbox creation
- validation rules
- Unit Tests → Mockito + JUnit 5
- Integration Tests → SpringBootTest
- Repository Tests → DataJpaTest

---

## 📂 Project Structure (Simplified)

```text
com.caorderapi
├── config
├── controller
├── dto
├── enums
├── feign
│   ├── adapter
│   ├── config
│   └── port
├── kafka
│   ├── consumer
│   ├── producer
│   └── model
├── model
├── repository
├── service
│   ├── impl
│   ├── scheduler
│   └── mapper
└── exception
```

---

## ⚖️ Trade-offs & Design Decisions

### Redis Inventory Reservation

* Improves inventory performance significantly
* Trade-off: requires cache synchronization strategy

---

### Outbox Pattern

* Guarantees reliable Kafka publishing
* Trade-off: additional DB writes

---

### Kafka Asynchronous Communication

* Improves scalability
* Trade-off: eventual consistency

---

### Scheduler-based Cleanup

* Simplifies stale order handling
* Trade-off: cleanup is periodic instead of real-time

---

## 🧠 Key Design Decisions

* Event-driven microservice architecture
* Reliable event publishing using Outbox Pattern
* Atomic stock reservation using Redis Lua scripts
* Resilient external service integrations
* Distributed-safe schedulers using ShedLock
* Database-backed status transition rules

---

## 📌 Notes

* Fully dockerized local environment
* Designed for horizontal scalability
* Easily extendable into multiple microservices
* Redis improves inventory throughput significantly

---

## ⚖️ Trade-offs & Design Decisions
✅ What Was Prioritized

The implementation intentionally focuses on the critical transactional backbone of an ordering system rather than implementing a full e-commerce platform.
Main focus areas:

* reliable order processing
* Performance
* Scalability
* Reliability
* production-grade backend engineering decisions
* Clean architecture
* Async integration support
---
## ❌ What Was Intentionally Simplified

This challenge intentionally does NOT implement:

* Full payment orchestration
* Real warehouse management
* Shipment tracking
* Customer authentication
* Multi-region deployment
* CQRS/Event sourcing
* Advanced fraud prevention
* Full inventory reconciliation

The goal was not full e-commerce coverage, but rather demonstrating:

how core order workflows can be implemented in a scalable and production-oriented way.
Enhancements or adding new attributes in order flow is not a big deal as it is designed to support future enhancements.
---
## 🚀 Scalability Considerations

The system is designed to scale horizontally.

---
## 🔗 Future Improvements
* CQRS + Event Sourcing
* Dedicated Inventory Service
* Saga orchestration
* Kubernetes deployment
* Observability stack
* Distributed tracing
* Multi-region failover
* Dead-letter queues
* Schema Registry
* Rate limiting
* API Gateway
---
## Future System Design
![future-ms-architecture.png](src/main/resources/images/future-ms-architecture.png)

---
## Future Order Api System Design
* ![future-order-api-flow.png](src/main/resources/images/future-order-api-flow.png)
---
## 📌 Assumptions
- Single currency support for simplicity
- Product catalog managed currently in order api but will be part of catalog microservice
- Inventory updates are eventually consistent
- Payment provider is external(mocked)
- Fulfillment provider is external(mocked)
- Authentication/authorization omitted intentionally
- Focus placed on backend transactional consistency

## ## 📊 Performance Testing Report

A comprehensive load test was executed against the **CA Order API** to evaluate system throughput, baseline latency characteristics, and stability thresholds under high-concurrency peak traffic conditions.

### 💻 Infrastructure & Test Environment

The entire testing ecosystem—including the application containers, downstream databases, and the load generator—was co-located on a single host machine via **Docker Compose**.

* **Host Hardware:** Apple Silicon M1 (8 Cores)
* **Host Physical Memory:** 16 GB RAM
* **Host Operating System:** macOS
* **Testing Profile:** 100 Concurrent Virtual Users (VUs) sustained for a fixed duration of 2 minutes.


![performance-test.png](src/main/resources/images/performance-test.png)