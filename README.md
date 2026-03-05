# 🚑 Emergency108 – Backend System

> Production-ready, fault-tolerant emergency dispatch backend built with Spring Boot, WebSockets (STOMP), Firebase Cloud Messaging, JPA, and MySQL.

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue?logo=mysql)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-blueviolet?logo=socketdotio)
![Firebase](https://img.shields.io/badge/Firebase-FCM-FFCA28?logo=firebase&logoColor=black)
![JWT](https://img.shields.io/badge/Auth-JWT-black?logo=jsonwebtokens)
![Docker](https://img.shields.io/badge/Docker-Containerised-2496ED?logo=docker&logoColor=white)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?logo=githubactions&logoColor=white)
![Deployed on Render](https://img.shields.io/badge/Deployed%20on-Render-46E3B7?logo=render&logoColor=white)
![DB on Aiven](https://img.shields.io/badge/Database-Aiven%20MySQL-FF5252?logo=aiven&logoColor=white)
![Mobile App](https://img.shields.io/badge/Mobile%20App-Flutter-02569B?logo=flutter&logoColor=white)
![Admin Panel](https://img.shields.io/badge/Admin%20Panel-React-61DAFB?logo=react&logoColor=black)
![License](https://img.shields.io/badge/License-MIT-green)
![Status](https://img.shields.io/badge/Status-Stable-success)

---

## 🌟 Overview

**Emergency108 Backend** is a **high-reliability, real-time emergency response system** that manages the full lifecycle of ambulance dispatching — from the moment an emergency is reported to safe patient delivery.

The system handles **automatic ambulance assignment, driver accept/reject flows, timeout-based reassignment, crash recovery**, and **concurrent access** safely using state-machine-driven workflows and domain event auditing.

Real-time driver location updates are streamed over **WebSockets (STOMP)**, push alerts are delivered to drivers and users via **Firebase Cloud Messaging (FCM)**, and all APIs are authenticated using **OTP-based JWT auth**.

---

## ✨ Key Features

- ⚡ **Full Emergency Lifecycle**
  - `CREATED → ASSIGNED → DISPATCHED → IN_PROGRESS → COMPLETED`
  - State transitions enforced by `EmergencyStateMachine` — no invalid jumps

- 📡 **Real-Time Location Tracking via WebSockets (STOMP)**
  - Drivers stream GPS over `/app/driver.location`
  - Admin dashboard receives live map updates at `/topic/live-locations`
  - Per-driver command channel at `/topic/driver/{driverId}`
  - SockJS fallback for browser compatibility

- 🔔 **Firebase Cloud Messaging (FCM) Push Notifications**
  - Dispatches push alerts to driver apps on new assignment
  - Notifies users on emergency status changes and cancellations
  - Graceful degradation — FCM failures never break core dispatch logic

- 🔁 **Automatic Retry & Timeout Handling**
  - Auto-reassigns if a driver rejects, times out, or goes offline
  - `AssignmentTimeoutScheduler` + `EmergencyAutoReassignService` run in background

- 🔒 **OTP-Based Authentication + JWT**
  - 6-digit OTP flow → JWT issued on verification
  - Roles: `ADMIN`, `DRIVER`, `PUBLIC`
  - Driver verification workflow (document upload → admin approval)

- 🗺️ **Geospatial Ambulance Dispatch**
  - Finds nearest available ambulance using Haversine distance calculation
  - Supports HTTP fallback for location updates (`PUT /api/driver/location`)

- ♻️ **Startup Crash Recovery**
  - Automatically restores incomplete emergencies after restarts

- 🧾 **Domain Event Auditing**
  - Non-blocking audit log of all system events; never impact core logic

- 📊 **Prometheus Metrics**
  - Exposed at `/actuator/prometheus`; tracks accepted/rejected/timeout counts

- 🧠 **System Readiness Guard**
  - Blocks API requests while recovery or initialization is in progress

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                  Emergency108 Backend                │
│                                                      │
│  REST APIs (13 controllers, ~40 endpoints)           │
│  WebSocket / STOMP (real-time location & dispatch)   │
│  JWT Auth (OTP flow, role guards)                    │
│  State Machines (Emergency + Assignment lifecycles)  │
│  Scheduled Jobs (timeouts, auto-reassign, cleanup)   │
│  Firebase FCM (push notifications)                   │
│  Prometheus Metrics + Actuator                       │
│                                                      │
│  DB: MySQL 8 on Aiven Cloud (SSL enforced)           │
│  Host: Render (containerised, auto-deploy)           │
└─────────────────────────────────────────────────────┘
```

**Tech Stack:**

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Real-time | WebSocket + STOMP (SockJS fallback) |
| Push Notifications | Firebase Admin SDK 9.2 (FCM) |
| Auth | OTP + JWT (JJWT 0.11.5) |
| Persistence | Spring Data JPA / Hibernate + MySQL 8 |
| Database Host | Aiven Managed MySQL (SSL required) |
| App Host | Render (Docker container) |
| Metrics | Micrometer + Prometheus |
| Build | Maven, Docker multi-stage |

---

## 📂 Project Structure

```text
com.emergency.emergency108
├── auth/            # OTP flow, JWT tokens, security filter, role guards
├── config/          # WebSocket, Firebase, Security, DB seeding
├── controller/      # 13 REST controllers
├── service/         # 21 business logic services
├── entity/          # 26 JPA entities
├── repository/      # 13 Spring Data JPA repositories
├── dto/             # API request / response objects
├── event/           # Domain event publishing & audit store
├── exception/       # Custom exceptions + global handler
├── metrics/         # Prometheus metrics collectors
├── resilience/      # Timeout, retry, circuit-breaker patterns
├── scheduler/       # Background jobs (timeouts, cleanup, reassignment)
├── system/          # Readiness checks, startup recovery
└── util/            # GeoCalc, state helpers, global exception handler
```

---

## 🌐 API Reference

### Authentication

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/auth/send-otp` | Send OTP to phone number |
| `POST` | `/api/auth/verify-otp` | Verify OTP → receive JWT |
| `POST` | `/api/legacy/login` | Dev-only quick login (returns JWT) |

### Emergencies

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/emergencies` | Create a new emergency |
| `POST` | `/api/emergencies/{id}/dispatch` | Assign nearest ambulance |
| `GET`  | `/api/emergencies/{id}/timeline` | Full event log |
| `GET`  | `/api/emergencies/{id}/track` | Track assigned ambulance |
| `POST` | `/api/emergencies/{id}/arrive` | Mark ambulance arrived |
| `POST` | `/api/emergencies/{id}/cancel` | Cancel emergency |
| `POST` | `/api/emergencies/{id}/ai-assessment` | AI severity triage |

### Driver

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/driver/start-shift` | Begin shift with ambulance |
| `POST` | `/api/driver/end-shift` | End shift |
| `GET`  | `/api/driver/current-session` | Active shift info |
| `PUT`  | `/api/driver/location` | Update location (HTTP fallback) |
| `POST` | `/api/driver/emergency/{id}/accept` | Accept dispatch |
| `POST` | `/api/driver/emergency/{id}/reject` | Reject dispatch |
| `POST` | `/api/driver/mark-patient-picked-up` | Confirm pickup |
| `POST` | `/api/driver/complete-mission` | Complete mission |
| `GET`  | `/api/driver/history` | Mission history |

### Admin

| Method | Path | Description |
|--------|------|-------------|
| `GET`  | `/api/admin/dashboard-stats` | KPI metrics |
| `GET`  | `/api/admin/active-emergencies` | Ongoing emergencies |
| `GET`  | `/api/admin/online-drivers` | Drivers in active shift |
| `GET`  | `/api/admin/pending-drivers` | Awaiting verification |
| `PUT`  | `/api/admin/verify-driver/{id}` | Approve driver |
| `PUT`  | `/api/admin/reject-driver/{id}` | Reject driver |
| `POST` | `/api/admin/emergencies/{id}/cancel` | Cancel as admin |

### Hospitals & Ambulances

| Method | Path | Description |
|--------|------|-------------|
| `GET`  | `/api/hospitals` | List hospitals |
| `POST` | `/api/hospitals` | Add hospital |
| `GET`  | `/api/ambulances` | List ambulances |
| `POST` | `/api/ambulances` | Create ambulance |

### WebSocket (STOMP)

**Endpoint:** `ws://<host>/ws` (SockJS fallback at `/ws`)

| Destination | Direction | Purpose |
|---|---|---|
| `/app/driver.location` | Client → Server | Driver streams GPS coordinates |
| `/topic/live-locations` | Server → Admin | Broadcast all active driver positions |
| `/topic/driver/{driverId}` | Server → Driver | Targeted dispatch commands |

---

## 🧠 How Key Features Work

### 📍 Geospatial Dispatch — Haversine Formula

Both ambulance dispatch and nearest-hospital selection use the **Haversine formula**, which calculates the great-circle distance between two points on the Earth's surface given their latitude/longitude coordinates.

$$
a = \sin^2\!\left(\frac{\Delta\phi}{2}\right) + \cos\phi_1 \cdot \cos\phi_2 \cdot \sin^2\!\left(\frac{\Delta\lambda}{2}\right)
$$
$$
d = 2R \cdot \arctan2\!\left(\sqrt{a},\, \sqrt{1 - a}\right)
$$

Where $\phi$ = latitude, $\lambda$ = longitude, $R$ = 6,371 km (Earth's mean radius).

**Used in three places:**

| Use Case | Implementation | Threshold |
|---|---|---|
| **Nearest ambulance** | `GeoUtil.distanceKm()` in `EmergencyDispatchService` — streams all online drivers and picks the one with minimum Haversine distance to the emergency location | No threshold; picks closest |
| **Nearest hospital** | Native SQL Haversine query in `HospitalRepository.findNearestHospitals()` — runs directly in MySQL, returns ordered results | Top N results |
| **Mission completion** | `DistanceCalculator.calculateDistance()` in `EmergencyAuthorizationService` — validates driver is physically at the hospital before allowing completion | Must be ≤ 100 metres |

```java
// GeoUtil.java — used for ambulance dispatch
double dLat = Math.toRadians(lat2 - lat1);
double dLon = Math.toRadians(lon2 - lon1);
double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
         + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
         * Math.sin(dLon / 2) * Math.sin(dLon / 2);
double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
return 6371 * c; // km
```

---

### 🚨 Emergency Creation — Self vs Someone Else

When a user creates an emergency via `POST /api/emergencies`, the backend immediately saves it with an `emergencyFor` value of `UNKNOWN` and starts a **30-second ownership decision window**.

```
User calls POST /api/emergencies
        │
        ▼
Emergency saved (status=CREATED, emergencyFor=UNKNOWN)
        │
        ├──► Admin panel notified via WebSocket
        ├──► Nearby helpers notified via FCM (within 3 km)
        │
        ▼
30-second window: PUT /api/emergencies/{id}/ownership
        │
        ├── emergencyFor = SELF
        │       └──► Emergency contacts are notified (family/friends alerted)
        │
        ├── emergencyFor = OTHER
        │       └──► Contact notifications skipped (caller is bystander)
        │
        └── No response within 30s
                └──► Scheduler auto-defaults to SELF (safety net)
```

**Rules:**
- Once set, `emergencyFor` is **immutable** — cannot be changed again
- Only `SELF` emergencies appear in the **Helping Hand** feed (respects victim privacy)
- The `EmergencyFor` enum has three states: `SELF`, `OTHER`, `UNKNOWN`

---

### 🤝 Helping Hand — Community First Responder Network

Helping Hand is an opt-in feature that turns nearby users into community first responders. Users who enable it share their background location and receive alerts when an emergency happens close to them.

```
User opts in (PUT /api/users/preferences/helping-hand)
        │
        ▼
User periodically posts location (POST /api/helping-hand/location)
        │
        ▼
Emergency is created (emergencyFor = SELF)
        │
        ▼
Backend finds all users within 3 km radius:
  Rules applied:
  ✔ Role must be PUBLIC
  ✔ helpingHandEnabled = true
  ✔ Location updated within last 24 hours
  ✔ Must not be the victim themselves
        │
        ▼
FCM batch push → "🚨 Emergency Nearby!"
  Payload: { type, emergencyId, latitude, longitude }
        │
        ▼
Helper opens app → GET /api/helping-hand/nearby-emergencies
  Privacy-filtered response (NearbyEmergencyDTO):
  ✔ Victim first name only (or "User nearby")
  ✔ Approximate location, distance, emergency type, status
  ✘ No phone number, address, or medical details exposed
  ✘ Capped at 5 nearest results, sorted by distance
```

**Privacy guarantees:** The `NearbyEmergencyDTO` deliberately strips all sensitive fields — only non-identifiable information is shared with helpers. Only `SELF`-type emergencies enter the feed; emergencies reported for someone else are excluded.

---

## 🔄 CI/CD Pipeline

Automated via **GitHub Actions** (`.github/workflows/render-deploy.yml`):

```
Push to master / Pull Request
        │
        ▼
① Build & Test (ubuntu-latest, JDK 21)
   └── mvn clean verify (runs all unit + integration tests)
        │
        ▼
② Deploy to Render
   └── Triggers Render deploy hook → Docker image built & deployed
```

- Tests must pass before any deploy
- Render auto-restarts the container on successful image push
- A second workflow (`keep_db_alive.yml`) runs on a schedule to ping the Aiven DB and prevent idle disconnects on free-tier

---

## ☁️ Deployment

### Render (Application Host)

The service is containerised and deployed on **[Render](https://render.com)** using the included `Dockerfile` (multi-stage Maven + JRE Alpine build).

- Port is dynamically injected via `$PORT` environment variable
- Automated deploys on every push to `master` via GitHub Actions CI/CD

### Aiven (Managed MySQL Database)

The production database runs on **[Aiven](https://aiven.io)** managed MySQL 8.

- SSL is enforced (`sslMode=REQUIRED`)
- Connection details are provided exclusively via environment variables — never hardcoded

---

## ⚙️ Configuration & Environment Variables

All secrets and environment-specific config are passed as environment variables. **Never commit credentials to the repository.**

| Variable | Required | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | Set to `prod` in production |
| `SPRING_DATASOURCE_URL` | Yes | Full JDBC URL (Aiven connection string) |
| `SPRING_DATASOURCE_USERNAME` | Yes | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | HMAC secret for signing JWT tokens |
| `FIREBASE_SERVICE_ACCOUNT_BASE64` | Yes | Base64-encoded Firebase service account JSON |
| `MAGIC_OTP_USER` | No | Static OTP for `PUBLIC` role (testing only) |
| `MAGIC_OTP_DRIVER` | No | Static OTP for `DRIVER` role (testing only) |
| `PORT` | No | Server port (injected by Render automatically) |

> **Important:** For Firebase, encode your service account JSON and set it as `FIREBASE_SERVICE_ACCOUNT_BASE64`. Never commit the raw JSON file to the repository.

### Application Profiles

| Profile | File | Use |
|---|---|---|
| *(default)* | `application.properties` | Shared base config |
| `local` | `application-local.properties` | Local development |
| `prod` | `application-prod.properties` | Production (Render + Aiven) |

---

## 🚀 Running Locally

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.x (local) or an Aiven trial cluster

### Steps

1. Clone the repository and navigate to the project root.
2. Create the database schema:
   ```bash
   mysql -u <user> -p < statementsqlforemergency.sql
   ```
3. Seed hospital data (optional):
   ```bash
   mysql -u <user> -p hackathon_108 < seed_hospitals.sql
   ```
4. Set environment variables:
   ```bash
   export SPRING_PROFILES_ACTIVE=local
   export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/hackathon_108
   export SPRING_DATASOURCE_USERNAME=<your_db_user>
   export SPRING_DATASOURCE_PASSWORD=<your_db_password>
   export JWT_SECRET=<your_jwt_secret>
   # Optional: for push notifications in local testing
   export FIREBASE_SERVICE_ACCOUNT_BASE64=<base64_encoded_service_account_json>
   ```
5. Start the application:
   ```bash
   ./mvnw spring-boot:run
   ```

The server starts on **port 8081** by default.

### Running with Docker

```bash
docker build -t emergency108 .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=<url> \
  -e SPRING_DATASOURCE_USERNAME=<user> \
  -e SPRING_DATASOURCE_PASSWORD=<password> \
  -e JWT_SECRET=<secret> \
  -e FIREBASE_SERVICE_ACCOUNT_BASE64=<base64_json> \
  emergency108
```

---

## 🧪 Testing

Import the included Postman collection for a full end-to-end test suite:

```
Emergency 108 - Complete Testing Suite.postman_collection.json
```

### Key Test Scenarios Covered

- ✅ Full emergency lifecycle (create → dispatch → complete)
- ✅ Multiple assignment retries and driver rejections
- ✅ Duplicate accept/reject calls (idempotency)
- ✅ Timeout-based auto-reassignment
- ✅ Crash and restart recovery
- ✅ Concurrent ambulance assignment (race conditions)
- ✅ Audit event persistence failures without system impact

---

## 🚀 Release

- **Version:** `v1.0.0`
- **Status:** Stable
- **Last Updated:** March 2026

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## 📬 Contact

I am a B.Tech CSE student passionate about building scalable Java applications.

[![Email](https://img.shields.io/badge/Email-anupamkushwaha639%40gmail.com-red?style=flat-square&logo=gmail)](mailto:anupamkushwaha639@gmail.com)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-anupamkushwaha85-blue?style=flat-square&logo=linkedin)](https://www.linkedin.com/in/anupamkushwaha85/)

---

### Built with ❤️ by Anupam Kushwaha

⭐ **If you find this project helpful, please give it a star!**
