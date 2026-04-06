# 🏨 LuxeStay — Hotel Management System

A full-stack, production-grade Hotel Management System built with **Spring Boot 3.2**, **PostgreSQL 16**, **Redis 7**, **JWT + Google OAuth2**, **PayPal Sandbox**, and **Thymeleaf** server-side templates. Designed to handle real-world hotel operations including room management, reservations, payments, staff workflows, and real-time notifications.

---

## 📋 Table of Contents

- [Feature Overview](#-feature-overview)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Data Model](#-data-model)
- [Quick Start (Docker)](#-quick-start-docker)
- [External Service Setup](#-external-service-setup)
- [Running Without Docker](#-running-without-docker)
- [Default Accounts](#-default-accounts)
- [Application Pages](#-application-pages)
- [REST API Reference](#-rest-api-reference)
- [Security Architecture](#-security-architecture)
- [Authentication Flows](#-authentication-flows)
- [PayPal Payment Flow](#-paypal-payment-flow)
- [WebSocket Real-Time Events](#-websocket-real-time-events)
- [Caching Strategy](#-caching-strategy)
- [Environment Variables](#-environment-variables)
- [Configuration Reference](#-configuration-reference)
- [Build & Deployment](#-build--deployment)
- [Troubleshooting](#-troubleshooting)

---

## ✨ Feature Overview

| Area | Features |
|---|---|
| **Authentication** | JWT access + refresh tokens, Google OAuth2 (Sign in with Google), BCrypt password hashing, OTP email verification, forgot/reset password flow |
| **Authorization** | Three-role RBAC — `GUEST`, `STAFF`, `ADMIN` — enforced via Spring Security method-level annotations |
| **Room Management** | Full CRUD, room types (Single/Double/Twin/Suite/Deluxe/Presidential), status management (Available/Occupied/Maintenance), date-range availability search with overlap prevention |
| **Bookings** | Create, cancel, confirm, check-in, check-out lifecycle; pessimistic locking to prevent double-booking; guest count + special requests |
| **Payments** | PayPal Sandbox — create PayPal order → redirect to PayPal → execute + capture → booking confirmed automatically |
| **Email Notifications** | Gmail SMTP — OTP codes, booking confirmation, cancellation notice, payment success receipt |
| **Real-Time** | WebSocket (STOMP over SockJS) — per-user notifications, admin broadcasts, room and booking status broadcasts |
| **Admin Panel** | Dashboard with revenue/occupancy stats, full room management, booking management, user directory with search, revenue reports |
| **Staff Panel** | Booking overview, check-in/out actions, room status updates |
| **Caching** | Redis cache (10-min TTL) for room listings and dashboard reports |
| **Rate Limiting** | Bucket4j token bucket — 100 requests/minute per IP |
| **API Docs** | Springdoc OpenAPI 2 / Swagger UI at `/swagger-ui.html` |
| **Actuator** | Health, info, metrics endpoints for monitoring |
| **Infrastructure** | Multi-stage Docker build + Docker Compose (PostgreSQL + Redis + App) |

---

## 🧱 Tech Stack

```
Backend        : Spring Boot 3.2.3, Java 17
Database       : PostgreSQL 16
Cache          : Redis 7
ORM            : Spring Data JPA + Hibernate (ddl-auto=update)
Connection Pool: HikariCP (max 20 connections)
Auth           : JWT via jjwt 0.12.3, Spring Security 6, Google OAuth2
Payments       : PayPal REST API (Sandbox)
Email          : JavaMail + Gmail SMTP (STARTTLS)
Real-Time      : Spring WebSocket + STOMP + SockJS
Frontend       : Thymeleaf 3 + Thymeleaf Spring Security Extras + vanilla CSS/JS
Mapping        : MapStruct 1.5.5
Boilerplate    : Lombok
Rate Limiting  : Bucket4j 7.6.0
API Docs       : Springdoc OpenAPI 2.3.0
Build          : Maven 3.9
Docker         : Multi-stage Dockerfile + Docker Compose 3.8
JVM Options    : -Xms256m -Xmx512m with container-aware RAM percentage
```

---

## 📁 Project Structure

```
hotel-management/
├── src/
│   └── main/
│       ├── java/com/hotel/
│       │   ├── HotelManagementApplication.java      # Spring Boot entry point
│       │   ├── config/
│       │   │   ├── AppConfig.java                   # Redis template, cache manager, CORS
│       │   │   ├── DataInitializer.java              # Seeds admin, staff, and 10 rooms on startup
│       │   │   ├── PayPalConfig.java                 # PayPal API context bean
│       │   │   ├── SecurityConfig.java               # Security filter chain, RBAC rules, OAuth2
│       │   │   └── WebSocketConfig.java              # STOMP broker + endpoint registration
│       │   ├── controller/
│       │   │   ├── AuthApiController.java            # POST /api/auth/** (register, login, OTP, refresh, logout, password)
│       │   │   ├── BookingController.java            # POST/GET/DELETE /api/bookings/**
│       │   │   ├── RoomController.java               # GET/POST/PUT/DELETE/PATCH /api/rooms/**
│       │   │   ├── PaymentController.java            # POST /api/payments/create + execute
│       │   │   ├── AdminController.java              # GET/POST/DELETE /api/admin/** [ADMIN only]
│       │   │   ├── StaffController.java              # GET /api/staff/** [ADMIN + STAFF]
│       │   │   └── PageController.java               # Thymeleaf page routes (MVC @Controller)
│       │   ├── dto/
│       │   │   ├── request/
│       │   │   │   ├── RegisterRequest.java          # username, name, email, password (validated)
│       │   │   │   ├── LoginRequest.java             # email, password
│       │   │   │   ├── VerifyOtpRequest.java         # email, otpCode, purpose
│       │   │   │   ├── RefreshTokenRequest.java      # refreshToken
│       │   │   │   ├── ForgotPasswordRequest.java    # email
│       │   │   │   ├── ResetPasswordRequest.java     # email, otpCode, newPassword
│       │   │   │   ├── BookingRequest.java           # roomId, checkInDate, checkOutDate, guestsCount, specialRequests
│       │   │   │   ├── RoomRequest.java              # roomNumber, type, price, status, description, maxOccupancy, imageUrl
│       │   │   │   └── PaymentRequest.java           # bookingId, method
│       │   │   └── response/
│       │   │       ├── ApiResponse.java              # Generic wrapper: success/message/data
│       │   │       ├── AuthResponse.java             # accessToken, refreshToken, user info
│       │   │       ├── BookingResponse.java          # Full booking with room + payment info
│       │   │       ├── RoomResponse.java             # Room details
│       │   │       ├── PaymentResponse.java          # Payment + approvalUrl
│       │   │       ├── UserResponse.java             # Safe user projection (no password)
│       │   │       ├── PageResponse.java             # Paginated wrapper: content, page, size, totalElements
│       │   │       └── ReportResponse.java           # Revenue, occupancy, booking stats
│       │   ├── entity/
│       │   │   ├── User.java                         # users table — id, username, email, password, role, verified, googleId
│       │   │   ├── Room.java                         # rooms table — roomNumber, type, price, status, maxOccupancy
│       │   │   ├── Booking.java                      # bookings table — user, room, dates, status, totalPrice, guestsCount
│       │   │   ├── Payment.java                      # payments table — booking, amount, status, method, transactionId, paypalOrderId
│       │   │   ├── RefreshToken.java                 # refresh_tokens table — token, expiry, revoked
│       │   │   └── Otp.java                          # otps table — email, otpCode, expiration, verified, purpose
│       │   ├── enums/
│       │   │   ├── Role.java                         # GUEST | STAFF | ADMIN
│       │   │   ├── RoomType.java                     # SINGLE | DOUBLE | TWIN | SUITE | DELUXE | PRESIDENTIAL
│       │   │   ├── RoomStatus.java                   # AVAILABLE | OCCUPIED | MAINTENANCE
│       │   │   ├── BookingStatus.java                # PENDING | CONFIRMED | CANCELLED | CHECKED_IN | CHECKED_OUT | COMPLETED
│       │   │   ├── PaymentStatus.java                # PENDING | PAID | FAILED | REFUNDED
│       │   │   └── PaymentMethod.java                # PAYPAL | CASH
│       │   ├── exception/
│       │   │   ├── GlobalExceptionHandler.java       # @RestControllerAdvice — maps all exceptions to ApiResponse
│       │   │   ├── BadRequestException.java          # 400
│       │   │   ├── ResourceNotFoundException.java    # 404
│       │   │   ├── RoomNotAvailableException.java    # 409
│       │   │   ├── InvalidTokenException.java        # 401
│       │   │   └── OtpException.java                 # 400 (expired / invalid OTP)
│       │   ├── mapper/
│       │   │   ├── BookingMapper.java                # MapStruct: Booking ↔ BookingResponse
│       │   │   ├── RoomMapper.java                   # MapStruct: Room ↔ RoomResponse
│       │   │   ├── PaymentMapper.java                # MapStruct: Payment ↔ PaymentResponse
│       │   │   └── UserMapper.java                   # MapStruct: User ↔ UserResponse
│       │   ├── repository/
│       │   │   ├── BookingRepository.java            # JPQL overlap query, pessimistic lock, count by status
│       │   │   ├── RoomRepository.java               # Available rooms query, filter by type/price, count by status
│       │   │   ├── UserRepository.java               # findByEmail, searchUsers (LIKE), existsByEmail/Username
│       │   │   ├── PaymentRepository.java            # findByBookingId, revenue aggregation
│       │   │   ├── RefreshTokenRepository.java       # findByToken, deleteByUser, revokeAll
│       │   │   └── OtpRepository.java                # findByEmailAndPurpose, deleteExpired
│       │   ├── security/
│       │   │   ├── CustomUserDetailsService.java     # Loads UserPrincipal from DB by email
│       │   │   ├── UserPrincipal.java                # Wraps User entity as Spring Security principal
│       │   │   ├── RestAuthenticationEntryPoint.java # Returns 401 JSON (not redirect) for API calls
│       │   │   ├── jwt/
│       │   │   │   ├── JwtTokenProvider.java         # generateToken, validateToken, extractClaims (jjwt 0.12)
│       │   │   │   └── JwtAuthenticationFilter.java  # OncePerRequestFilter — reads JWT from header or cookie
│       │   │   └── oauth2/
│       │   │       ├── CustomOAuth2UserService.java  # Loads/creates user from Google profile
│       │   │       ├── CustomOAuth2User.java         # Wraps OAuth2User with local User entity
│       │   │       ├── OAuth2AuthenticationSuccessHandler.java  # Issues JWT on Google login success
│       │   │       ├── GoogleOAuth2UserInfo.java      # Extracts sub, email, name, picture from Google claims
│       │   │       ├── OAuth2UserInfo.java            # Abstract base for provider info
│       │   │       └── OAuth2UserInfoFactory.java     # Factory: "google" → GoogleOAuth2UserInfo
│       │   ├── service/
│       │   │   ├── OtpService.java                   # Generate, save, validate, clean OTPs
│       │   │   ├── EmailService.java                 # Send OTP, booking confirmation, cancellation, payment emails
│       │   │   └── impl/
│       │   │       ├── AuthServiceImpl.java          # register, login, verifyOtp, refreshToken, logout, forgotPassword, resetPassword
│       │   │       ├── BookingServiceImpl.java       # createBooking (pessimistic lock), getUserBookings, cancelBooking, checkIn, checkOut, confirmBooking
│       │   │       ├── RoomServiceImpl.java          # getAllRooms, getAvailableRooms, createRoom, updateRoom, deleteRoom, updateStatus (with cache evict)
│       │   │       ├── PaymentServiceImpl.java       # createPayment (PayPal order), executePayment (capture), update booking status
│       │   │       └── ReportServiceImpl.java        # getDashboardReport — revenue, occupancy rate, booking stats
│       │   └── websocket/
│       │       └── NotificationService.java          # sendToUser, sendToAdmins, broadcastRoomUpdate, broadcastBookingUpdate
│       └── resources/
│           ├── application.yml                       # Full configuration (all secrets via env vars)
│           ├── static/
│           │   ├── css/main.css                      # Full responsive stylesheet (~29KB)
│           │   └── js/app.js                         # JWT management, fetch helpers, WebSocket client, UI logic (~18KB)
│           └── templates/
│               ├── fragments.html                    # Navbar, footer, toast, modal (Thymeleaf fragments)
│               ├── auth/
│               │   ├── login.html                    # Email/password login + Google OAuth2 button
│               │   ├── register.html                 # Registration form
│               │   ├── verify-otp.html               # OTP input (registration + password reset)
│               │   ├── forgot-password.html          # Request OTP for password reset
│               │   └── reset-password.html           # Set new password after OTP
│               ├── user/
│               │   ├── home.html                     # Room listings with search/filter
│               │   ├── room-detail.html              # Room detail page
│               │   ├── booking.html                  # Booking form
│               │   ├── my-bookings.html              # Guest's booking history
│               │   ├── payment-success.html          # PayPal success landing
│               │   └── payment-cancel.html           # PayPal cancel landing
│               └── admin/
│                   ├── dashboard.html                # Stats, charts, quick actions
│                   ├── rooms.html                    # Room CRUD UI
│                   ├── bookings.html                 # All bookings with filters
│                   ├── users.html                    # User directory
│                   └── reports.html                  # Revenue and occupancy reports
├── docker/
│   └── init.sql                                      # Creates pg_trgm extension; JPA handles table creation
├── Dockerfile                                        # Multi-stage: Maven builder → JRE-only runtime (non-root user)
├── docker-compose.yml                                # PostgreSQL 16 + Redis 7 + Spring Boot app
├── .env                                              # Your local secrets (DO NOT commit)
└── pom.xml                                           # All Maven dependencies
```

---

## 🗄️ Data Model

### Entities & Relationships

```
User (1) ──────────────── (N) Booking
User (1) ──────────────── (N) RefreshToken
Booking (1) ───────────── (1) Payment
Room (1) ──────────────── (N) Booking
```

### Table Summary

| Table | Key Columns | Indexes |
|---|---|---|
| `users` | id, username\*, email\*, name, password, role, is\_verified, google\_id | `idx_users_email`, `idx_users_username` |
| `rooms` | id, room\_number\*, type, price, status, description, max\_occupancy, image\_url | `idx_rooms_status`, `idx_rooms_type`, `idx_rooms_price` |
| `bookings` | id, user\_id, room\_id, check\_in\_date, check\_out\_date, status, total\_price, guests\_count | `idx_bookings_user_id`, `idx_bookings_room_id`, `idx_bookings_dates`, `idx_bookings_status` |
| `payments` | id, booking\_id, amount, status, method, transaction\_id, paypal\_order\_id, approval\_url | `idx_payments_booking_id`, `idx_payments_transaction_id` |
| `refresh_tokens` | id, user\_id, token, expiry\_date, revoked | — |
| `otps` | id, email, otp\_code, expiration\_time, verified, purpose | — |

\* = unique constrained

### Enums

| Enum | Values |
|---|---|
| `Role` | `GUEST`, `STAFF`, `ADMIN` |
| `RoomType` | `SINGLE`, `DOUBLE`, `TWIN`, `SUITE`, `DELUXE`, `PRESIDENTIAL` |
| `RoomStatus` | `AVAILABLE`, `OCCUPIED`, `MAINTENANCE` |
| `BookingStatus` | `PENDING`, `CONFIRMED`, `CANCELLED`, `CHECKED_IN`, `CHECKED_OUT`, `COMPLETED` |
| `PaymentStatus` | `PENDING`, `PAID`, `FAILED`, `REFUNDED` |
| `PaymentMethod` | `PAYPAL`, `CASH` |

### Seeded Room Data

On first startup, `DataInitializer` seeds **10 rooms**:

| Room | Type | Price/Night | Max Guests |
|---|---|---|---|
| 101, 102 | SINGLE | $89 | 1 |
| 201, 202 | DOUBLE | $149 | 2 |
| 203 | TWIN | $139 | 2 |
| 301, 302 | SUITE | $299 | 3 |
| 401, 402 | DELUXE | $219 | 2 |
| 501 | PRESIDENTIAL | $599 | 6 |

---

## 🚀 Quick Start (Docker)

### Prerequisites

- Docker Engine + Docker Compose v2
- A Gmail account (for SMTP — requires 2FA + App Password)
- A PayPal Developer account (for Sandbox payments)
- A Google Cloud project (for OAuth2 — optional, login/register still works without it)

---

### Step 1 — Clone and configure

```bash
git clone <your-repo-url>
cd hotel-management

# Copy the environment file
cp .env.example .env   # or copy .env manually
```

Open `.env` and fill in all required values (see [Environment Variables](#-environment-variables) below).

To generate a strong JWT secret:
```bash
openssl rand -base64 64
```

---

### Step 2 — Start the stack

```bash
docker compose up --build
```

This starts three containers:

| Container | Image | Port |
|---|---|---|
| `hotel_postgres` | `postgres:16-alpine` | `5432` |
| `hotel_redis` | `redis:7-alpine` | `6379` |
| `hotel_app` | Built from `Dockerfile` | `8080` |

The app container waits for both Postgres and Redis health checks before starting.

> **Note:** First build takes ~2–3 minutes as Maven downloads all dependencies inside the Docker layer. Subsequent builds are fast due to the dependency cache layer in the Dockerfile.

---

### Step 3 — Access the app

| URL | Description |
|---|---|
| `http://localhost:8080` | Home — room listings |
| `http://localhost:8080/auth/login` | Login |
| `http://localhost:8080/auth/register` | Register |
| `http://localhost:8080/admin` | Admin dashboard |
| `http://localhost:8080/swagger-ui.html` | Interactive API docs |
| `http://localhost:8080/api-docs` | OpenAPI JSON |
| `http://localhost:8080/actuator/health` | Health check |

---

## 🔧 External Service Setup

### Gmail SMTP (App Password)

The app uses Gmail SMTP with STARTTLS on port 587. You **must** use an App Password — your real Gmail password will not work.

1. Go to [myaccount.google.com](https://myaccount.google.com) → Security
2. Enable **2-Step Verification** if not already done
3. Go to **App passwords** → Generate a new app password
4. Copy the 16-character password (format: `xxxx xxxx xxxx xxxx`)
5. Set in `.env`:
   ```
   SMTP_EMAIL=your.email@gmail.com
   SMTP_PASSWORD=xxxx xxxx xxxx xxxx
   ```

### PayPal Sandbox

1. Go to [developer.paypal.com](https://developer.paypal.com) and log in
2. Navigate to **My Apps & Credentials** → **Create App**
3. Give it a name, select "Merchant" type
4. Copy the **Client ID** and **Secret** from Sandbox credentials
5. Create a **Sandbox Personal account** (to act as the customer paying)
6. Set in `.env`:
   ```
   PAYPAL_CLIENT_ID=your-sandbox-client-id
   PAYPAL_CLIENT_SECRET=your-sandbox-secret
   PAYPAL_MODE=sandbox
   ```

### Google OAuth2 (Sign in with Google)

1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Create a new project (or use existing)
3. Navigate to **APIs & Services → Credentials**
4. Click **Create Credentials → OAuth 2.0 Client ID**
5. Application type: **Web application**
6. Add to **Authorized redirect URIs**:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
7. Copy the **Client ID** and **Client Secret**
8. Set in `.env`:
   ```
   GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=GOCSPX-your-secret
   ```

---

## 💻 Running Without Docker

If you prefer to run the app directly on your machine:

```bash
# 1. Start PostgreSQL locally (must be running on port 5432)
# 2. Start Redis locally (must be running on port 6379)

# 3. Export environment variables (or set them in your IDE run config)
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=hotel_db
export DB_USERNAME=postgres
export DB_PASSWORD=your-password
export JWT_SECRET=your-jwt-secret-base64
export JWT_ACCESS_EXPIRATION=86400000
export JWT_REFRESH_EXPIRATION=604800000
export SMTP_EMAIL=your@gmail.com
export SMTP_PASSWORD="xxxx xxxx xxxx xxxx"
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export PAYPAL_CLIENT_ID=your-paypal-client-id
export PAYPAL_CLIENT_SECRET=your-paypal-secret
export PAYPAL_MODE=sandbox
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=
export SERVER_PORT=8080
export FRONTEND_URL=http://localhost:8080
export OAUTH2_REDIRECT_URIS=http://localhost:8080/oauth2/redirect

# 4. Run
mvn spring-boot:run
```

JPA will auto-create all tables on first run (`ddl-auto=update`). The `DataInitializer` will seed the admin account, staff account, and 10 sample rooms.

---

## 👤 Default Accounts

These accounts are automatically created by `DataInitializer` on first startup.

| Role | Email | Password |
|---|---|---|
| Admin | `admin@luxestay.com` | `Admin@1234` |
| Staff | `staff@luxestay.com` | `Admin@1234` |

> ⚠️ **Change these passwords immediately in any non-local deployment.**

---

## 🖥️ Application Pages

### Public Pages
| URL | Description |
|---|---|
| `/` or `/index` | Home — room listings with search |
| `/rooms/{id}` | Room detail page |
| `/auth/login` | Login (email/password + Google) |
| `/auth/register` | New user registration |
| `/auth/verify-otp` | OTP verification |
| `/auth/forgot-password` | Password reset request |
| `/auth/reset-password` | Set new password |

### Authenticated User Pages
| URL | Description |
|---|---|
| `/bookings/new?roomId=` | Create a booking for a room |
| `/bookings/my` | My booking history |
| `/payments/success` | PayPal payment success landing |
| `/payments/cancel` | PayPal payment cancel landing |

### Admin Pages
| URL | Description |
|---|---|
| `/admin` | Dashboard with stats and charts |
| `/admin/rooms` | Room management (CRUD) |
| `/admin/bookings` | All bookings with status filters |
| `/admin/users` | User directory with search |
| `/admin/reports` | Revenue and occupancy reports |

---

## 🔌 REST API Reference

All endpoints are under `/api/`. Full interactive docs available at `http://localhost:8080/swagger-ui.html`.

---

### Authentication — `/api/auth`

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | No | Register new user; sends OTP email |
| `POST` | `/api/auth/login` | No | Login → returns `accessToken` + `refreshToken`; sets `access_token` HttpOnly cookie |
| `POST` | `/api/auth/verify-otp` | No | Verify OTP code; issues tokens on success |
| `POST` | `/api/auth/refresh` | No | Exchange refresh token for new access token |
| `POST` | `/api/auth/logout` | No | Revokes refresh token; clears cookie |
| `POST` | `/api/auth/forgot-password` | No | Sends OTP to email for password reset |
| `POST` | `/api/auth/reset-password` | No | Reset password using OTP + new password |

**Example — Register:**
```json
POST /api/auth/register
{
  "username": "johndoe",
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass@123"
}
```

**Example — Login:**
```json
POST /api/auth/login
{
  "email": "john@example.com",
  "password": "SecurePass@123"
}
```

---

### Rooms — `/api/rooms`

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `GET` | `/api/rooms` | No | All rooms (paginated, sorted by price by default) |
| `GET` | `/api/rooms/available` | No | Available rooms for a date range with optional filters |
| `GET` | `/api/rooms/{id}` | No | Single room by ID |
| `POST` | `/api/rooms` | ADMIN | Create a new room |
| `PUT` | `/api/rooms/{id}` | ADMIN | Update a room |
| `DELETE` | `/api/rooms/{id}` | ADMIN | Delete a room |
| `PATCH` | `/api/rooms/{id}/status` | ADMIN / STAFF | Update room status only |

**Query Parameters for `/api/rooms/available`:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `checkIn` | `LocalDate` (ISO) | Yes | Check-in date e.g. `2025-06-15` |
| `checkOut` | `LocalDate` (ISO) | Yes | Check-out date |
| `type` | `RoomType` enum | No | Filter by room type |
| `minPrice` | `BigDecimal` | No | Minimum price per night |
| `maxPrice` | `BigDecimal` | No | Maximum price per night |
| `page` | int | No | Page index (default 0) |
| `size` | int | No | Page size (default 10) |
| `sortBy` | String | No | Sort field (default `price`) |

---

### Bookings — `/api/bookings`

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/bookings` | Authenticated | Create booking (pessimistic lock on room) |
| `GET` | `/api/bookings/my` | Authenticated | Get current user's bookings (paginated) |
| `GET` | `/api/bookings/{id}` | Authenticated | Get single booking |
| `DELETE` | `/api/bookings/{id}` | Authenticated | Cancel booking (user can cancel own; admin can cancel any) |
| `POST` | `/api/bookings/{id}/checkin` | ADMIN / STAFF | Mark booking as CHECKED\_IN |
| `POST` | `/api/bookings/{id}/checkout` | ADMIN / STAFF | Mark booking as CHECKED\_OUT |

**Example — Create Booking:**
```json
POST /api/bookings
Authorization: Bearer <token>
{
  "roomId": 3,
  "checkInDate": "2025-07-01",
  "checkOutDate": "2025-07-05",
  "guestsCount": 2,
  "specialRequests": "Late check-in please"
}
```

---

### Payments — `/api/payments`

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/payments/create` | Authenticated | Create PayPal order for a booking → returns `approvalUrl` |
| `POST` | `/api/payments/execute` | No | Execute/capture payment after PayPal redirect (`?token=...&PayerID=...`) |
| `GET` | `/api/payments/execute` | No | Same as above — handles PayPal GET redirect |

---

### Admin — `/api/admin` *(ADMIN role only)*

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/admin/bookings` | All bookings; optional `?status=` filter; paginated |
| `GET` | `/api/admin/users` | All users; optional `?search=` text search; paginated |
| `GET` | `/api/admin/reports` | Revenue, occupancy rate, booking counts by status |
| `POST` | `/api/admin/bookings/{id}/confirm` | Confirm a pending booking |
| `DELETE` | `/api/admin/bookings/{id}` | Cancel any booking |

---

### Staff — `/api/staff` *(ADMIN and STAFF roles)*

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/staff/bookings` | All bookings with optional status filter |
| `GET` | `/api/staff/dashboard` | Room counts, occupancy rate, booking counts by status |

---

## 🔒 Security Architecture

### Filter Chain

```
Incoming Request
    │
    ▼
JwtAuthenticationFilter (OncePerRequestFilter)
    ├── Reads token from:
    │     1. Authorization: Bearer <token> header
    │     2. access_token HttpOnly cookie (fallback)
    ├── Validates signature + expiry via JwtTokenProvider
    └── Sets Authentication in SecurityContextHolder
    │
    ▼
Spring Security AuthorizationFilter
    ├── PUBLIC: /, /auth/**, /api/auth/**, /api/rooms (GET), /oauth2/**, /ws/**, /swagger-ui/**, /actuator/health
    ├── AUTHENTICATED: /api/bookings/**, /api/payments/**
    ├── ADMIN + STAFF: /api/staff/**, /api/bookings/*/checkin, /api/bookings/*/checkout
    └── ADMIN only: /api/admin/**, POST/PUT/DELETE /api/rooms/**
```

### Token Details

| Token | Duration | Storage |
|---|---|---|
| Access Token | 24 hours (configurable via `JWT_ACCESS_EXPIRATION`) | Authorization header or HttpOnly cookie |
| Refresh Token | 7 days (configurable via `JWT_REFRESH_EXPIRATION`) | Database (`refresh_tokens` table); revocable |

### Password Security

- BCrypt with strength 12 (`BCryptPasswordEncoder(12)`)
- Passwords never stored in plain text
- Google OAuth2 users have no password (null field allowed)

### CORS

Configured in `AppConfig` to allow all origins with credentials. **Tighten this to your specific domain before production deployment.**

---

## 🔑 Authentication Flows

### Standard Registration + OTP Verification

```
1. POST /api/auth/register
       └─► User saved (unverified)
           6-digit OTP generated, stored, emailed

2. POST /api/auth/verify-otp  { email, otpCode, purpose: "REGISTRATION" }
       └─► OTP validated (expires in 10 min)
           User marked verified
           Access token + refresh token returned
```

### Login

```
POST /api/auth/login
    └─► Credentials validated
        Access token set as HttpOnly cookie
        Access + refresh tokens returned in body
```

### Token Refresh

```
POST /api/auth/refresh  { refreshToken: "..." }
    └─► Refresh token found in DB (not revoked, not expired)
        New access token issued
```

### Password Reset

```
1. POST /api/auth/forgot-password  { email }
       └─► OTP emailed

2. POST /api/auth/reset-password  { email, otpCode, newPassword }
       └─► OTP validated
           Password updated
```

### Google OAuth2 Login

```
1. User clicks "Sign in with Google"
2. Redirected to Google OAuth consent screen
3. Google redirects back to /login/oauth2/code/google
4. CustomOAuth2UserService:
       ├── If user exists (by googleId or email): loads user
       └── If new user: creates User record (role=GUEST, verified=true)
5. OAuth2AuthenticationSuccessHandler:
       └─► Issues JWT access + refresh tokens
           Redirects to FRONTEND_URL + /oauth2/redirect?token=...
```

---

## 💳 PayPal Payment Flow

```
1. User creates booking → status: PENDING

2. POST /api/payments/create  { bookingId, method: "PAYPAL" }
       └─► Backend calls PayPal API to create order
           Returns { approvalUrl: "https://www.sandbox.paypal.com/checkoutnow?token=..." }

3. Frontend redirects user to approvalUrl
       └─► User logs in with PayPal Sandbox Personal account
           Approves payment

4. PayPal redirects to /payments/success?token=ORDER_ID&PayerID=PAYER_ID

5. POST /api/payments/execute?token=ORDER_ID&PayerID=PAYER_ID
       └─► Backend calls PayPal capture API
           transactionId stored in payments table
           Payment status → PAID
           Booking status → CONFIRMED
           Room status → OCCUPIED
           Email confirmation sent
           WebSocket notification sent to user + admins
```

---

## 🔔 WebSocket Real-Time Events

**Connection endpoint:** `ws://localhost:8080/ws` (SockJS fallback enabled)

**STOMP broker prefix:** `/topic`

| Topic | Recipients | Triggered By |
|---|---|---|
| `/topic/user.{username}` | Specific user | Booking created, cancelled, payment success/fail, check-in, check-out |
| `/topic/admins` | Admin/Staff | New booking created, payment received |
| `/topic/rooms` | All connected clients | Room status change |
| `/topic/bookings` | All connected clients | Booking status change |

### Notification Payload

```json
{
  "type": "BOOKING_CONFIRMED",
  "message": "Your booking has been confirmed.",
  "data": { ... },
  "timestamp": "2025-06-15T14:30:00"
}
```

The frontend `app.js` connects to WebSocket on page load, subscribes to the user-specific topic using the authenticated user's username, and displays toast notifications.

---

## ⚡ Caching Strategy

Redis is used as the cache provider (`spring.cache.type=redis`) with a default TTL of 10 minutes.

| Cache Name | What is Cached | Evicted When |
|---|---|---|
| `rooms` | Room list queries | Room created, updated, deleted, or status changed |
| `reports` | Dashboard report data | Any booking or payment change |

Cache keys are automatically managed by Spring's `@Cacheable` / `@CacheEvict` annotations in `RoomServiceImpl` and `ReportServiceImpl`.

---

## 🌍 Environment Variables

All secrets and configuration are injected via environment variables. Never hardcode these values.

| Variable | Description | Example |
|---|---|---|
| `DB_HOST` | PostgreSQL host | `postgres` (Docker) / `localhost` (local) |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `hotel_db` |
| `DB_USERNAME` | DB user | `hotel_user` |
| `DB_PASSWORD` | DB password | `StrongPass@123` |
| `JWT_SECRET` | Base64 JWT signing key (min 64 bytes) | Output of `openssl rand -base64 64` |
| `JWT_ACCESS_EXPIRATION` | Access token TTL in ms | `86400000` (24h) |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL in ms | `604800000` (7d) |
| `GOOGLE_CLIENT_ID` | Google OAuth2 Client ID | `xxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 Client Secret | `GOCSPX-...` |
| `SMTP_EMAIL` | Gmail address for sending emails | `you@gmail.com` |
| `SMTP_PASSWORD` | Gmail App Password (16 chars) | `xxxx xxxx xxxx xxxx` |
| `PAYPAL_CLIENT_ID` | PayPal Sandbox Client ID | `AYBRm...` |
| `PAYPAL_CLIENT_SECRET` | PayPal Sandbox Secret | `EL4fa...` |
| `PAYPAL_MODE` | PayPal mode | `sandbox` or `live` |
| `REDIS_HOST` | Redis host | `redis` (Docker) / `localhost` (local) |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password | `StrongRedis@123!` |
| `SERVER_PORT` | App HTTP port | `8080` |
| `FRONTEND_URL` | Base URL of the app | `http://localhost:8080` |
| `OAUTH2_REDIRECT_URIS` | Allowed OAuth2 redirect URIs | `http://localhost:8080/oauth2/redirect` |
| `OTP_EXPIRATION` | OTP validity in minutes | `10` |

---

## ⚙️ Configuration Reference (`application.yml`)

| Setting | Value | Notes |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `update` | Auto-creates/updates tables on startup |
| `spring.cache.redis.time-to-live` | `600000` ms | 10 min cache TTL |
| `hikari.maximum-pool-size` | `20` | Max DB connections |
| `hikari.minimum-idle` | `5` | Min idle connections |
| `rate-limit.capacity` | `100` | Token bucket capacity |
| `rate-limit.refill-tokens` | `100` | Tokens refilled per minute |
| `logging.level.com.hotel` | `DEBUG` | Application logs |
| `management.endpoints.web.exposure.include` | `health, info, metrics` | Exposed actuator endpoints |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Swagger UI URL |

---

## 📦 Build & Deployment

### Run Tests

```bash
mvn test
```

### Build Production JAR

```bash
mvn clean package -DskipTests
java -jar target/hotel-management-1.0.0.jar
```

### Docker Multi-Stage Build

The `Dockerfile` uses two stages:

1. **Builder stage** (`maven:3.9.6-eclipse-temurin-17-alpine`):
    - Copies `pom.xml` first and runs `mvn dependency:go-offline` to cache the dependency layer
    - Then copies source code and builds the JAR with `-DskipTests`

2. **Runtime stage** (`eclipse-temurin:17-jre-alpine`):
    - Only the JRE (no JDK/Maven) for a smaller image
    - Creates a non-root user `hoteluser` for security
    - JVM options: `-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`
    - Health check: `wget http://localhost:8080/actuator/health`
    - Exposes port `8080`

---

## 🛠️ Troubleshooting

| Problem | Cause | Solution |
|---|---|---|
| App fails to connect to database | `DB_HOST` is wrong inside Docker | Keep `DB_HOST=postgres` (the Docker service name), not `localhost` |
| Emails not being sent | Wrong Gmail credentials | Use a Gmail **App Password**, not your real password. Requires 2FA to be enabled |
| PayPal redirect broken | `FRONTEND_URL` mismatch | Set `FRONTEND_URL` to the exact host/port the app is running on |
| Google login fails | Missing redirect URI | Add `http://localhost:8080/login/oauth2/code/google` to your Google OAuth2 authorized redirect URIs |
| Redis connection refused | Password mismatch | Ensure `REDIS_PASSWORD` in `.env` matches the `redis-server --requirepass` value in `docker-compose.yml` |
| OTP not arriving | Email issue | Check your spam folder; verify the Gmail account has 2FA + App Password configured |
| 401 on all API requests | JWT not being sent | Ensure `Authorization: Bearer <token>` header is present, or the `access_token` cookie is set |
| Double-booking occurs | Caching issue | Redis cache may be stale; cache eviction on room updates should handle this automatically |
| Port 8080 already in use | Port conflict | Change `SERVER_PORT` in `.env` and update the port mapping in `docker-compose.yml` |
| Container exits immediately | Missing required env var | Check `docker compose logs hotel_app` — Spring Boot will fail fast on missing required properties like `JWT_SECRET` |

---

## 📄 License

MIT License — free to use, modify, and distribute.

---

*Built with Spring Boot 3.2, Java 17, and ❤️ — LuxeStay Hotel Management System*