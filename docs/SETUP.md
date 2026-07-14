# 🛠️ Keyz Backend Installation & Setup Guide

Welcome to the **Keyz Backend Setup Guide**. This document outlines the step-by-step instructions needed to configure, compile, run, and test the Keyz real estate platform backend on any local or production system.

---

## 📋 Table of Contents
1. [Prerequisites](#-prerequisites)
2. [Environment Configuration](#-environment-configuration)
3. [Local Development Setup](#-local-development-setup)
4. [Running with Docker](#-running-with-docker)
5. [Testing the Application](#-testing-the-application)
6. [External Service Setup](#-external-service-setup)
7. [Troubleshooting & FAQs](#-troubleshooting--faqs)

---

## ⚙️ Prerequisites

Before you begin, ensure you have the following installed and configured on your system:

| Tool / Technology | Required Version | Purpose |
|---|---|---|
| **Java Development Kit (JDK)** | `17` or higher | Main runtime and compiler for Spring Boot 3.x |
| **Maven** | `3.9+` | Build automation and dependency management (or use included `./mvnw`) |
| **PostgreSQL** | `15` or higher | Primary relational database (local instance or managed like Neon DB) |
| **Docker** (Optional) | `20.10+` | Containerized builds and deployment |
| **Git** | `Any` | Version control and cloning the repository |

---

## 🔑 Environment Configuration

The backend reads configuration settings from environment variables or `application-local.properties`. 

### 1. The Configuration Keys
Ensure you have the following secrets and keys ready:

*   **Database (`DB_*`):** Connection URL, username, and password for PostgreSQL.
*   **JWT Security (`JWT_*`):** A strong 256-bit hex string for signing JWT tokens and its expiration time (e.g., `86400000` for 24 hours).
*   **AWS S3 Credentials (`AWS_*`):** Required for uploading and streaming property video walkthroughs.
*   **Resend Email (`RESEND_*`):** For sending transactional emails like OTPs and password reset links.
*   **Jitsi Meet (`JITSI_*`):** Application ID and RSA private key to sign JaaS JWTs for private live video rooms.

### 2. Setting Up `application-local.properties`
Create or update the file `src/main/resources/application-local.properties` with your actual development secrets:

```properties
# Database Configuration (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/keyzdb
spring.datasource.username=postgres
spring.datasource.password=your_secure_password

# HikariCP Connection Pool Settings
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=0
spring.datasource.hikari.max-lifetime=300000
spring.datasource.hikari.idle-timeout=180000
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.keepalive-time=60000

# Logging Configurations
logging.level.root=WARN
logging.level.com.example.backend=INFO
logging.level.org.springframework.web=WARN

# JWT Authentication
application.security.jwt.secret-key=YOUR_64_CHARACTER_HEX_STRING_HERE
application.security.jwt.expiration=86400000

# Resend Email Integration
resend.api-key=re_yourResendApiKeyHere
resend.from-email=support@yourdomain.com

# AWS S3 Integration
aws.access-key-id=YOUR_AWS_ACCESS_KEY_ID
aws.secret-access-key=YOUR_AWS_SECRET_ACCESS_KEY
aws.region=eu-west-1
aws.s3.bucket-name=keyz-walkthroughs

# Jitsi JaaS (Virtual 3D Tours)
jitsi.app-id=vpaas-magic-cookie-yourAppIdHere
jitsi.app-secret=YOUR_MULTI_LINE_JITSI_RSA_PRIVATE_KEY_HERE
jitsi.server-url=https://8x8.vc

# Dojah API Verification (KYC & Biometric Checks)
dojah.app-id=YOUR_DOJAH_APP_ID
dojah.api-key=YOUR_DOJAH_API_KEY
dojah.base-url=https://sandbox.dojah.io
```

> [!IMPORTANT]
> Never commit `application-local.properties` or any raw secrets to public Git repositories. The project's `.gitignore` is pre-configured to exclude local configuration files.

---

## 💻 Local Development Setup

Follow these commands to clone, build, and run the project locally on your machine.

### Step 1: Clone the Repository
```bash
git clone https://github.com/JBR-Ltd/keyz-backend.git
cd keyz-backend
```

### Step 2: Initialize the Relational Database
Create a PostgreSQL database named `keyzdb`:
```sql
CREATE DATABASE keyzdb;
```
If using a cloud provider like **Neon DB**, copy the database connection string and replace the database URL in your local properties.

### Step 3: Compile and Build
Build the project and download all dependencies using Maven:
*   **Windows (CMD/PowerShell):**
    ```cmd
    mvnw.cmd clean package -DskipTests
    ```
*   **macOS / Linux:**
    ```bash
    chmod +x mvnw
    ./mvnw clean package -DskipTests
    ```

### Step 4: Run the Application
Start the Spring Boot dev server:
*   **Windows:**
    ```cmd
    mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
    ```
*   **macOS / Linux:**
    ```bash
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
    ```

The application will start on port **`8080`** by default (configured via `server.port=${PORT:8080}`). 

Verify it is running by visiting:
*   **Status Check:** `http://localhost:8080/error` (returns default Spring JSON error instead of crashing)
*   **Swagger API Docs:** `http://localhost:8080/swagger-ui/index.html`

---

## 🐳 Running with Docker

The codebase is equipped with a multi-stage `Dockerfile` designed to optimize image size and build layers.

### Step 1: Create a Environment File (`.env`)
Create a `.env` file in the root of the project:
```env
PORT=8080
DB_URL=jdbc:postgresql://your-neon-url:5432/neondb?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=your_db_password
JWT_SECRET=404E635266556A788E32723575389776F413F4428472B4B6250645367566B5678
JWT_EXPIRATION=86400000
RESEND_API_KEY=re_yourResendApiKey
RESEND_FROM_EMAIL=support@yourdomain.com
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=yourAwsSecretKey
AWS_REGION=eu-west-1
AWS_S3_BUCKET_NAME=keyz-walkthroughs
JITSI_APP_ID=vpaas-magic-cookie-xxx
JITSI_APP_SECRET=yourJitsiSecretKey
JITSI_SERVER_URL=https://8x8.vc
```

### Step 2: Build the Docker Image
```bash
docker build -t keyz-backend .
```

### Step 3: Run the Container
```bash
docker run -d -p 8080:8080 --env-file .env --name keyz-backend-app keyz-backend
```

Check the logs to verify startup:
```bash
docker logs -f keyz-backend-app
```

---

## 🧪 Testing the Application

We maintain a comprehensive suite of unit and integration tests covering security, JWT filters, controller mappings, and business rules (e.g. review blockers, rate limit tests).

To run all tests:
```bash
./mvnw clean test
```

To run a specific test class:
```bash
./mvnw test -Dtest=ReviewControllerTest
```

---

## 🌐 External Service Setup

To enable all features of the Keyz platform, set up these free-tier external accounts:

### 1. Amazon S3 (Video Storage)
1. Log in to the [AWS Console](https://aws.amazon.com/).
2. Create an S3 Bucket with Public Access allowed (or configure bucket policies to allow public read access for streamed walkthrough videos).
3. Create an IAM User with `AmazonS3FullAccess` or a custom policy restricting actions to `PutObject` on your bucket.
4. Copy the **Access Key ID** and **Secret Access Key**.

### 2. Resend (Email Delivery)
1. Go to [resend.com](https://resend.com/) and register a free developer account.
2. Generate an API Key under the **API Keys** tab.
3. Validate your custom domain under **Domains** to get higher sending limits, or use `onboarding@resend.dev` for testing emails sent to your registered account email.

### 3. Jitsi JaaS (Live 3D Virtual Tours)
1. Sign up on [Jitsi 8x8 JaaS](https://jaas.8x8.vc/).
2. Create an Application in your JaaS dashboard and copy the **App ID**.
3. Generate an RSA key pair in the dashboard. Download the **Private Key** (in PKCS#8 format) and paste it into the `JITSI_APP_SECRET` variable.

### 4. Dojah API (KYC/KYB, NIN & BVN Verification)
1. Go to [Dojah Portal](https://dojah.io/) and register a developer account.
2. In your dashboard, copy your **App ID** and **API Key / Authorization Secret** from the developer credentials section.
3. Configure the sandbox base URL: `https://sandbox.dojah.io` for development testing.
4. If these parameters are omitted, the backend will automatically fallback to a detailed mock engine allowing you to fully test NIN, BVN, and Selfie checks locally.

---

## ❓ Troubleshooting & FAQs

### 🛑 DB Connection Rejected / SSL Exception
*   **Cause:** Managed databases like Neon require SSL.
*   **Solution:** Make sure your connection string contains `?sslmode=require` or specify SSL configuration properties in the JDBC URL.

### 🛑 JWT Signature Verification Failed
*   **Cause:** The JWT signing key is not long enough or has changed across restarts.
*   **Solution:** Generate a secure 256-bit (64-character) hex key. Avoid changing it dynamically if you want client logins to persist across server hot-reloads.

### 🛑 S3 Upload Access Denied (`403 Forbidden`)
*   **Cause:** The IAM user credentials do not have the right permissions, or the S3 bucket is blocking public uploads.
*   **Solution:** Check the S3 Bucket CORS policy and Block Public Access settings. Ensure the IAM user has `s3:PutObject` and `s3:PutObjectAcl` permissions.

### 🛑 Jitsi Signature Generation Failure
*   **Cause:** The RSA private key provided in the settings has bad formatting (linebreaks, missing headers).
*   **Solution:** Ensure the private key has proper headers: `-----BEGIN PRIVATE KEY-----` and `-----END PRIVATE KEY-----` and contains no invalid spaces.
