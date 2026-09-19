# 🏨 Starlight Hospitality – Microservices Reservation System

A **Microservices-based Multi-Property Hospitality Reservation & Management System** developed for **Starlight Stays & Resorts**.

The system allows users to authenticate securely, search available rooms, make reservations, and prevents double-booking for overlapping dates.

---

## 📌 Project Overview

The system is designed using **Spring Boot Microservices Architecture**.

Instead of developing the complete application as one large service, the system is divided into independent services based on their responsibilities.

### Main Services

* 👤 **User Service** – User registration, login and JWT authentication
* 🏨 **Room Service** – Properties, rooms and room availability
* 📅 **Booking Service** – Reservations and double-booking prevention
* 🚪 **API Gateway** – Single entry point and dynamic request routing
* 🔎 **Eureka Server** – Service discovery

---

## 🏗️ Architecture

```text
                    ┌─────────────────┐
                    │      Client     │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   API Gateway   │
                    │     :8080       │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
       ┌────────────┐ ┌────────────┐ ┌────────────┐
       │User Service│ │Room Service│ │  Booking   │
       │   :8081    │ │   :8082    │ │  Service   │
       └─────┬──────┘ └─────┬──────┘ │   :8083    │
             │              │         └──────┬─────┘
             ▼              ▼                │
        MySQL DB       MySQL DB             │
                                             │
                              ┌──────────────┘
                              │ OpenFeign
                              ▼
                       ┌────────────┐
                       │Room Service│
                       └────────────┘

                    ┌─────────────────┐
                    │  Eureka Server  │
                    │      :8761      │
                    └─────────────────┘
```

---

## 🔧 Technologies Used

| Technology           | Purpose                         |
| -------------------- | ------------------------------- |
| Java 21              | Programming language            |
| Spring Boot          | Microservice development        |
| Spring Cloud         | Distributed system support      |
| Spring Cloud Gateway | API Gateway                     |
| Netflix Eureka       | Service discovery               |
| Spring Security      | API security                    |
| JWT                  | Authentication                  |
| OpenFeign            | Inter-service communication     |
| MySQL                | Persistent data storage         |
| Maven                | Build and dependency management |
| Git & GitHub         | Version control                 |
| Postman / PowerShell | API testing                     |

---

## 🧩 Microservices

### 1. Eureka Server

**Port:** `8761`

Responsible for service registration and discovery.

Services registered with Eureka:

```text
API-GATEWAY
USER-SERVICE
ROOM-SERVICE
BOOKING-SERVICE
```

Dashboard:

```text
http://localhost:8761
```

---

### 2. API Gateway

**Port:** `8080`

The Gateway acts as the **single entry point** for client requests.

| Request              | Destination     |
| -------------------- | --------------- |
| `/api/users/**`      | USER-SERVICE    |
| `/api/auth/**`       | USER-SERVICE    |
| `/api/properties/**` | ROOM-SERVICE    |
| `/api/rooms/**`      | ROOM-SERVICE    |
| `/api/bookings/**`   | BOOKING-SERVICE |

The Gateway uses:

```text
lb://SERVICE-NAME
```

so services are located dynamically through Eureka.

---

### 3. User Service

**Port:** `8081`

Responsibilities:

* User registration
* User login
* JWT token generation
* Password hashing using BCrypt
* User authorization
* Protected user APIs

Example login:

```http
POST /api/auth/login
```

Example:

```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

---

### 4. Room Service

**Port:** `8082`

Responsibilities:

* Property management
* Room management
* Room search
* Room availability
* Availability block management

Example:

```http
GET /api/rooms/search
```

Example room:

```text
Room: 101
Type: DELUXE
Price: ₹2500/night
Capacity: 2
```

---

### 5. Booking Service

**Port:** `8083`

Responsibilities:

* Create reservations
* Retrieve bookings
* Validate room information
* Prevent overlapping reservations
* User-specific booking access
* Booking cancellation

Booking Service communicates with Room Service using **OpenFeign**.

Example:

```text
Booking Service
       │
       │ OpenFeign
       ▼
Room Service
       │
       ▼
Room Details
```

---

## 🔐 JWT Authentication

The application uses **JWT-based stateless authentication**.

Authentication flow:

```text
User Login
    ↓
User Service
    ↓
JWT Token Generated
    ↓
Client Sends Bearer Token
    ↓
API Gateway
    ↓
Protected Microservice
    ↓
Request Authorized
```

Example header:

```http
Authorization: Bearer <JWT_TOKEN>
```

Passwords are protected using **BCrypt hashing**.

---

## 🛡️ Double-Booking Prevention

The Booking Service checks whether another confirmed booking exists for the same room and overlapping dates.

Example:

```text
Existing Booking:
Room 101
Oct 1 → Oct 3

New Booking:
Room 101
Oct 2 → Oct 4
```

The dates overlap, so the new booking is rejected.

Response:

```text
409 Conflict
```

This prevents the same room from being booked by multiple users for overlapping dates.

---

## 🗄️ Database Design

Separate MySQL databases are used for the services.

```text
starlight_user_db
starlight_room_db
starlight_booking_db
```

### Room Database

Main tables:

```text
properties
rooms
room_availability_blocks
```

The database provides persistent storage for property and room information.

---

## 🚀 Running the Project

### Prerequisites

Install:

* Java 21
* Maven 3.9+
* MySQL 8+
* Git

Check Java:

```powershell
java -version
```

Check Maven:

```powershell
mvn -version
```

---

## ▶️ Startup Order

Start the services in this order:

```text
1. Eureka Server
2. User Service
3. Room Service
4. Booking Service
5. API Gateway
```

### Service URLs

```text
Eureka Server  → http://localhost:8761
API Gateway    → http://localhost:8080
User Service   → http://localhost:8081
Room Service   → http://localhost:8082
Booking Service→ http://localhost:8083
```

---

## 🧪 Testing Flow

### 1. Login

```http
POST http://localhost:8080/api/auth/login
```

### 2. Access Protected User API

```http
GET http://localhost:8080/api/users/1
```

with:

```http
Authorization: Bearer <token>
```

### 3. Search Rooms

```http
GET http://localhost:8080/api/rooms/search
```

### 4. Create Booking

```http
POST http://localhost:8080/api/bookings
```

Example:

```json
{
  "userId": 1,
  "roomId": 1,
  "checkIn": "2026-10-01",
  "checkOut": "2026-10-03",
  "guests": 2
}
```

### 5. Test Double Booking

Send the same booking again.

Expected result:

```text
409 Conflict
```

### 6. Test Authentication

Call:

```http
GET http://localhost:8080/api/bookings
```

without a JWT.

Expected result:

```text
403 Forbidden
```

---

## 📊 Key Features Demonstrated

* ✅ Microservices architecture
* ✅ Service discovery using Eureka
* ✅ Dynamic API Gateway routing
* ✅ JWT authentication
* ✅ BCrypt password security
* ✅ OpenFeign communication
* ✅ Room search
* ✅ Reservation management
* ✅ Double-booking prevention
* ✅ MySQL persistence
* ✅ Health monitoring
* ✅ Modular service design

---

## 📁 Project Structure

```text
Starlight-Hospitality/
│
├── eureka-server/
│
├── api-gateway/
│
├── user-service/
│
├── room-service/
│
├── booking-service/
│
├── pom.xml
│
├── PROJECT_SUBMISSION.md
│
└── README.md
```

---

## 🎓 Academic Project

**Course:** 24SDCS03R – SOA Programming and Microservices

**Academic Year:** 2026–2027 Odd Semester

**Project:** PS004 – Multi-Property Hospitality Reservation & Management System

**Company:** Starlight Stays & Resorts

**Team:** PS04-S06-02

---

## 👩‍💻 Project Objective

The main objective of this project is to demonstrate how a hospitality reservation platform can be designed using **Service-Oriented Architecture and Microservices**.

The project demonstrates how independent services can communicate through an API Gateway and service discovery while maintaining secure authentication, persistent data storage, and reliable reservation validation.

---

## 🔮 Future Enhancements

Possible future improvements include:

* Online payment integration
* Email/SMS booking notifications
* Admin dashboard
* Advanced room availability management
* Dynamic seasonal pricing
* Reservation cancellation/refund workflow
* Docker containerization
* Cloud deployment
* Centralized logging and monitoring

---

## 📜 License

This project was developed for academic/educational purposes.
