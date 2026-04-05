# 🏨 LuxeStay — Hotel Management System

A full-stack, production-grade Hotel Management System built with **Spring Boot 3**, **PostgreSQL**, **Redis**, **JWT + OAuth2**, **PayPal**, and **Thymeleaf** server-side templates.

---

## ✨ Feature Overview

| Area | Features |
|---|---|
| **Auth** | JWT (access + refresh tokens), Google OAuth2, BCrypt, OTP email verification, forgot/reset password |
| **Roles** | `GUEST`, `STAFF`, `ADMIN` with Spring Security method-level RBAC |
| **Rooms** | CRUD, type/price/status filtering, availability search with overlap prevention |
| **Bookings** | Create, cancel, check-in, check-out with **pessimistic locking** to prevent double-booking |
| **Payments** | PayPal Sandbox integration — create order → redirect → execute → confirm |
| **Email** | Gmail SMTP — OTP, booking confirmation, cancellation, payment success |
| **Real-time** | WebSocket (STOMP/SockJS) notifications for users and admins |
| **Admin** | Dashboard, room management, booking management, user directory, revenue reports with charts |
| **Caching** | Redis cache for rooms and reports |
| **Infra** | Docker Compose with PostgreSQL + Redis + Spring Boot app |

---

## 🧱 Tech Stack

```
Backend  : Spring Boot 3.2, Java 17
Database : PostgreSQL 16
Cache    : Redis 7
Auth     : JWT (jjwt 0.12), Spring Security 6, Google OAuth2
Payments : PayPal REST SDK (Sandbox)
Email    : JavaMail + Gmail SMTP
Realtime : Spring WebSocket + STOMP + SockJS
Frontend : Thymeleaf + vanilla CSS/JS
Build    : Maven 3.9
Docker   : Docker Compose
```

---

## 📁 Project Structure

```
hotel-management/
├── src/main/java/com/hotel/
│   ├── config/              # Security, Redis, WebSocket, PayPal configs
│   ├── controller/          # REST + Thymeleaf page controllers
│   ├── dto/
│   │   ├── request/         # Validated input DTOs
│   │   └── response/        # Output DTOs (no entity exposure)
│   ├── entity/              # JPA entities with auditing
│   ├── enums/               # Role, RoomStatus, BookingStatus, etc.
│   ├── exception/           # Custom exceptions + global handler
│   ├── mapper/              # MapStruct mappers
│   ├── repository/          # JPA repos with JPQL + pessimistic locking
│   ├── security/
│   │   ├── jwt/             # JwtTokenProvider, JwtAuthFilter
│   │   └── oauth2/          # Google OAuth2 flow
│   ├── service/impl/        # All business logic
│   └── websocket/           # NotificationService
├── src/main/resources/
│   ├── templates/           # Thymeleaf HTML pages
│   │   ├── auth/            # login, register, verify-otp, forgot/reset password
│   │   ├── user/            # home, room-detail, booking, my-bookings
│   │   ├── admin/           # dashboard, rooms, bookings, users, reports
│   │   └── fragments.html   # navbar, footer, modal, toast
│   ├── static/
│   │   ├── css/main.css     # Full responsive stylesheet
│   │   └── js/app.js        # JWT management, API helpers, WebSocket, UI
│   └── application.yml      # All config (zero hardcoded secrets)
├── docker/
│   └── init.sql             # DB indexes + seed data (admin user + 10 rooms)
├── Dockerfile               # Multi-stage build
├── docker-compose.yml       # PostgreSQL + Redis + App
├── .env.example             # Environment variable template
└── pom.xml                  # All dependencies
```

---

## 🚀 Quick Start

### Prerequisites

- Docker + Docker Compose
- A Gmail account (for SMTP)
- A PayPal Developer account (for Sandbox)
- A Google Cloud project (for OAuth2)

---

### 1. Clone & Configure

```bash
git clone <your-repo-url>
cd hotel-management

# Create your .env from the template
cp .env.example .env
```

Open `.env` and fill in every value:

```bash
# Generate a strong JWT secret (run this):
openssl rand -base64 64
```

---

### 2. External Service Setup

#### Gmail SMTP (App Password)
1. Go to [myaccount.google.com](https://myaccount.google.com)
2. Security → 2-Step Verification → App passwords
3. Generate a 16-character app password
4. Set `SMTP_EMAIL=your@gmail.com` and `SMTP_PASSWORD=xxxx xxxx xxxx xxxx`

#### PayPal Sandbox
1. Go to [developer.paypal.com](https://developer.paypal.com)
2. My Apps & Credentials → Create App
3. Copy **Client ID** and **Secret** into `.env`
4. Create a Sandbox Personal account to test payments

#### Google OAuth2
1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Create project → APIs & Services → Credentials → OAuth 2.0 Client ID
3. Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google`
4. Copy Client ID and Secret into `.env`

---

### 3. Run with Docker Compose

```bash
docker compose up --build
```

This starts:
- **PostgreSQL** on port 5432
- **Redis** on port 6379
- **Spring Boot app** on port 8080

First startup takes ~90 seconds (Maven downloads dependencies inside Docker).

---

### 4. Access the App

| URL | Description |
|---|---|
| `http://localhost:8080` | Home (room listings) |
| `http://localhost:8080/auth/login` | Login page |
| `http://localhost:8080/auth/register` | Register page |
| `http://localhost:8080/admin` | Admin dashboard |
| `http://localhost:8080/swagger-ui.html` | API docs (Swagger UI) |

#### Default Admin Account (seeded by init.sql)
```
Email    : admin@luxestay.com
Password : Admin@1234
```

#### Default Staff Account
```
Email    : staff@luxestay.com
Password : Admin@1234
```

---

## 🔌 API Reference

All REST endpoints are under `/api/`. Full interactive docs at `/swagger-ui.html`.

### Auth
```
POST /api/auth/register          Register new user
POST /api/auth/login             Login → access + refresh tokens
POST /api/auth/verify-otp        Verify OTP (registration / Google login / reset)
POST /api/auth/refresh           Refresh access token
POST /api/auth/logout            Revoke refresh token
POST /api/auth/forgot-password   Send password reset OTP
POST /api/auth/reset-password    Reset password with OTP
```

### Rooms
```
GET  /api/rooms                              All rooms (paginated)
GET  /api/rooms/available?checkIn=&checkOut= Available rooms for dates
GET  /api/rooms/{id}                         Room detail
POST /api/rooms                              Create room [ADMIN]
PUT  /api/rooms/{id}                         Update room [ADMIN]
DELETE /api/rooms/{id}                       Delete room [ADMIN]
PATCH /api/rooms/{id}/status                 Update room status [ADMIN/STAFF]
```

### Bookings
```
POST   /api/bookings             Create booking (with pessimistic lock)
GET    /api/bookings/my          My bookings
GET    /api/bookings/{id}        Booking detail
DELETE /api/bookings/{id}        Cancel booking
POST   /api/bookings/{id}/checkin    Check in [ADMIN/STAFF]
POST   /api/bookings/{id}/checkout   Check out [ADMIN/STAFF]
```

### Payments
```
POST /api/payments/create    Create PayPal order → returns approvalUrl
POST /api/payments/execute   Execute after PayPal redirect (paymentId + PayerID)
```

### Admin
```
GET  /api/admin/bookings     All bookings (filterable by status)
GET  /api/admin/users        All users (searchable)
GET  /api/admin/reports      Revenue, occupancy, booking stats
POST /api/admin/bookings/{id}/confirm   Confirm a booking
DELETE /api/admin/bookings/{id}         Cancel a booking
```

---

## 🔒 Security Architecture

```
Request
  └─► JwtAuthenticationFilter
        ├── Reads token from Authorization header OR access_token cookie
        ├── Validates JWT signature + expiry
        └── Sets SecurityContext

Spring Security
  ├── Public: /auth/**, /api/rooms (GET), /oauth2/**, /static/**
  ├── Authenticated: /api/bookings/**, /api/payments/**
  ├── ADMIN: /api/admin/**, /admin/**
  └── ADMIN/STAFF: /api/bookings/*/checkin, /api/bookings/*/checkout
```

### Refresh Token Flow
1. Access token expires (15 min)
2. Client sends refresh token to `POST /api/auth/refresh`
3. Server validates stored token (not revoked, not expired)
4. Issues new access token
5. On logout: refresh token is revoked in DB

### OTP Flow
- 6-digit random code, stored hashed-equivalent, expires in 10 min
- Required after: registration, Google login, password reset
- Scheduled cleanup removes expired OTPs every hour

---

## 💳 PayPal Flow

```
1. POST /api/payments/create
       └─► Backend creates PayPal order (merchant account)
           Returns { approvalUrl }

2. Browser redirects to approvalUrl
       └─► User logs in with PayPal Personal account
           Approves payment

3. PayPal redirects to /payments/success?paymentId=...&PayerID=...

4. POST /api/payments/execute
       └─► Backend executes payment
           Stores transactionId
           Marks booking CONFIRMED
           Sends confirmation email + WebSocket notification
```

---

## 🔔 WebSocket Events

Connect at: `ws://localhost:8080/ws` (SockJS)

| Topic | Who receives | When |
|---|---|---|
| `/user/queue/notifications` | Specific user | Booking created/cancelled, payment success/fail, check-in/out |
| `/topic/admin` | Admin/Staff | New bookings, payments received |
| `/topic/rooms` | Everyone | Room status changes |

---

## 🗄️ Database Schema

```
users          — id, username*, email*, name, password, role, is_verified
rooms          — id, room_number*, type, price, status, description, max_occupancy
bookings       — id, user_id, room_id, check_in_date, check_out_date, status, total_price
payments       — id, booking_id, amount, status, method, transaction_id, paypal_order_id
refresh_tokens — id, user_id, token, expiry_date, revoked
otps           — id, email, otp_code, expiration_time, verified, purpose

* unique indexed
```

Key performance indexes:
- Composite index on `bookings(room_id, check_in_date, check_out_date)` filtered by active statuses
- GIN trigram indexes on `users(name, email)` for fast search
- Functional indexes on `LOWER(email)`, `LOWER(username)`

---

## 🛠️ Local Development (Without Docker)

```bash
# 1. Start PostgreSQL and Redis locally
# 2. Set environment variables in your shell or IDE
export DB_HOST=localhost
export JWT_SECRET=your-secret-here
# ... etc

# 3. Run the app
mvn spring-boot:run
```

---

## 🏗️ Running Tests

```bash
mvn test
```

---

## 📦 Build Production JAR

```bash
mvn clean package -DskipTests
java -jar target/hotel-management-1.0.0.jar
```

---

## 🔧 Troubleshooting

| Problem | Solution |
|---|---|
| App can't connect to DB | Check DB_HOST=postgres (not localhost) inside Docker |
| Email not sending | Verify Gmail App Password (not your real password) |
| PayPal redirect fails | Ensure FRONTEND_URL matches your actual host |
| Google login broken | Add `http://localhost:8080/login/oauth2/code/google` to authorized redirects |
| Redis connection error | Check REDIS_PASSWORD matches docker-compose |
| OTP not arriving | Check spam folder; verify SMTP_EMAIL has 2FA + App Password enabled |

---

## 📄 License

MIT License — free to use, modify, and distribute.

---

*Built with Spring Boot 3.2, Java 17, and ❤️*
