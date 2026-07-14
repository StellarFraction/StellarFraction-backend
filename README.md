# 🏠 Keyz Real Estate Platform — Backend Core

Welcome to the backend engine of the **Keyz** real estate platform. This server handles property listings, secure real estate bookings, user transactions, interactive 3D Matterport tours, virtual live consultations, and compliance workflows.

---

## 📚 Technical Documentation Hub

To help developers quickly onboard and understand the backend design, we have created dedicated guides for each core facet of the application:

*   **⚡ [Installation & Setup Guide](./docs/SETUP.md):** Step-by-step instructions to run the application locally or via Docker, database seeding, and configuring external service APIs (S3, Resend, Jitsi).
*   **🏛️ [System Architecture & Design](./docs/ARCHITECTURE.md):** Detailed architectural design patterns, package structures, transactional rules (booking-gated reviews, automated rating engines), rate-limiting subsystems, and visual Mermaid sequence flows.
*   **📖 [REST API Reference & Swagger UI](./docs/API_DOCS.md):** Full list of available HTTP endpoints, path/query variables, JSON request-response payload envelopes, status codes, and instructions for interactive browser testing via Swagger UI.

---

## Overview
Keyz is a comprehensive platform facilitating both short-term property rentals (Airbnb style) and long-term property sales. The backend is built using Spring Boot, prioritizing high security, modularity, and clean architecture.

## Architectural Design
The codebase follows a strictly typed, modular layered architecture. All business logic is abstracted behind interfaces.

* **Controller Layer** (`com.example.backend.controller`): RESTful endpoints that consume standardized DTOs.
* **Service Layer** (`com.example.backend.service`): Interfaces defining business capabilities.
* **Implementation Layer** (`com.example.backend.service.impl`): Concrete business logic enforcing rules like KYB verification and rate limiting.
* **Repository Layer** (`com.example.backend.repository`): Spring Data JPA interfaces for database access.
* **Security & Configurations** (`com.example.backend.security`, `com.example.backend.config`): JWT validation, device fingerprinting, and Bucket4j rate limiting.

## Security Features
Security is treated as a first-class citizen across the application.
1. **Device Fingerprinting:** Every authentication attempt must supply an `X-Device-Fingerprint` header. We map fingerprints to users to track login behaviors and detect anomalies.
2. **Rate Limiting:** IP-based throttling is enforced via `Bucket4j`. All `/api/**` routes are restricted to a maximum of 100 requests per minute.
3. **Role-Based Access Control (RBAC):** Strict JWT checks ensure that only users with the `LANDLORD` or `AGENT` roles can list properties.
4. **Resend Email Integration:** Secure OTP/Token delivery for email onboarding and password recovery is fully integrated with Resend.
5. **Email Verification Onboarding:** All newly registered users are initialized in a pending status. A 6-digit numeric OTP is immediately dispatched to their inbox, and login is strictly blocked until their email is verified.

## Core Workflows

### 1. Authentication & Recovery
* **POST /api/auth/register**: Registers a new user with email set to pending verification.
* **POST /api/auth/verify-email**: Verifies a registered user's email by consuming their 6-digit OTP code to activate their account.
* **POST /api/auth/login**: Authenticates a user (Requires `X-Device-Fingerprint` and verified email account status). Returns an `AuthResponse` containing a JWT.
* **POST /api/auth/forgot-password**: Triggers a secure password reset email via Resend containing a user-friendly 6-digit OTP.
* **POST /api/auth/reset-password**: Consumes the 6-digit OTP to securely update the password.

### 2. KYB & Automated Verification Trust Gates
To ensure a trusted, friction-free marketplace, verifications are automated:
* **POST /api/verification/kyb**: Submits identity verification documents.
* **POST /api/verification/agent**: Submits NIN, BVN, and selfie for automated Smile ID biometric trust checks.
* **POST /api/verification/property/verify**: Submits photo for geofenced proof-of-presence (distance <= 30m) auto-verification.
* **POST /api/verification/property/verify-bill**: Submits utility bill for automated OCR address and owner checks using AWS Textract.
* **POST /api/verification/payout/setup** & **/confirm**: Automated micro-deposit bank account verification.

### 3. Property Management
* **POST /api/properties/create**: Allows verified landlords or agents to list a property (`FOR_RENT` for landlords, `FOR_SALE` for agents).
* **GET /api/properties/sale**: Fetches verified properties available for purchase.
* **GET /api/properties/rent**: Fetches verified properties available for rent.
* **GET /api/properties/all**: Aggregates all verified properties across both statuses.

### 4. Transactions (Booking & Offers)
* **POST /api/bookings**: Submits a rental reservation for a `FOR_RENT` property. Calculates total price based on date ranges.
* **POST /api/offers**: Submits a binding financial offer on a `FOR_SALE` property.

### 5. Trust & Ratings
* **POST /api/reviews**: Submits a 1-5 rating and comment. A strict validation rule ensures the user has a `COMPLETED` booking history for the property before allowing a review. 
* **Seller Aggregate Score**: The platform automatically calculates and updates a global trust score for hosts based on aggregated property reviews.

## Standardized Responses
All API endpoints return a standardized `ApiResponse` structure to ensure consistent client-side parsing:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

## Getting Started

### Prerequisites
- **JDK 17 or higher**: The project uses Spring Boot 3.x.
- **Maven 3.9+**: For building and dependency management (or use the provided `./mvnw`).
- **PostgreSQL**: A running instance (local or remote like Neon).
- **AWS S3 Bucket**: For storing property video walkthroughs.
- **Resend API Key**: For sending emails (OTP and password recovery).
- **Jitsi (JaaS) Account**: For virtual property tours.

### Local Development Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/JBR-Ltd/keyz-backend.git
   cd keyz-backend
   ```

2. **Environment Configuration:**
   Create an `application-local.properties` file in `src/main/resources/` or set the following environment variables:

   | Variable | Description |
   |---|---|
   | `DB_URL` | PostgreSQL connection URL (e.g., `jdbc:postgresql://localhost:5432/neondb`) |
   | `DB_USERNAME` | Database username |
   | `DB_PASSWORD` | Database password |
   | `JWT_SECRET` | 64-char hex string for signing JWTs |
   | `RESEND_API_KEY` | API key from resend.com |
   | `AWS_ACCESS_KEY_ID` | AWS access key for S3 |
   | `AWS_SECRET_ACCESS_KEY` | AWS secret key for S3 |
   | `AWS_S3_BUCKET_NAME` | S3 bucket name for uploads |
   | `JITSI_APP_ID` | JaaS Application ID |
   | `JITSI_APP_SECRET` | JaaS Private Key (RSA) |

3. **Build and Run:**
   Using the Maven wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
   The application will start on `http://localhost:8080`.

4. **API Documentation:**
   Explore the interactive Swagger UI at:
   `http://localhost:8080/swagger-ui.html`

   For a detailed frontend integration guide, see [API_DOCS.md](./docs/API_DOCS.md).

### Running with Docker

The project includes a multi-stage `Dockerfile` for production-ready builds.

1. **Build the image:**
   ```bash
   docker build -t keyz-backend .
   ```

2. **Run the container:**
   ```bash
   docker run -p 8080:8080 --env-file .env keyz-backend
   ```

### Testing
Execute the test suite (includes security and logic validation):
```bash
./mvnw clean test
```

## Deployment
This backend is optimized for deployment on **Render** using Docker. Ensure all environment variables are configured in the Render Dashboard. The database should be a managed PostgreSQL instance (e.g., Neon).