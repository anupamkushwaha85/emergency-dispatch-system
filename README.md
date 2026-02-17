# 🚑 Emergency108 – Backend System

> Production-ready, fault-tolerant emergency dispatch backend built with Spring Boot, JPA, and MySQL.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue)
![Status](https://img.shields.io/badge/Status-Stable-success)
![License](https://img.shields.io/badge/License-Proprietary-red)

---

## 🌟 Overview

**Emergency108 Backend** is a **high-reliability emergency response system** designed to manage ambulance dispatching with strict correctness guarantees.

The system handles **assignment retries, driver rejections, timeouts, crash recovery**, and **concurrent access** safely.  
It is built using **domain-driven design**, **state-driven workflows**, and **event-based auditing** to ensure resilience under real-world failure conditions.

---

## ✨ Key Features

- ⚡ **Robust Emergency Lifecycle**
  - `CREATED → ASSIGNED → DISPATCHED → IN_PROGRESS → COMPLETED`

- 🔁 **Automatic Retry & Timeout Handling**
  - Reassigns ambulances if drivers reject or fail to respond within timeout

- 🔒 **Concurrency Safe**
  - Uses optimistic & pessimistic locking where required

- ♻️ **Startup Recovery**
  - Restores incomplete emergencies after crashes or restarts

- 📊 **Built-in Metrics**
  - Tracks assignment accepted / rejected / timeout statistics

- 🧾 **Domain Event Auditing**
  - Non-blocking audit persistence that never breaks core logic

- 🧠 **System Readiness Guard**
  - Blocks requests while recovery or initialization is in progress

---

## 🏗️ Architecture Highlights

- Spring Boot 3.2
- JPA / Hibernate
- State-machine–driven transitions
- Domain events (audit-safe)
- Scheduled background jobs
- Metrics via Micrometer
- RESTful APIs

---

## 📂 Project Structure

```text
com.hackathon.emergency108
├── controller    # REST APIs
├── service       # Core business logic
├── entity        # JPA entities
├── repository    # Data access layer
├── event         # Domain events & auditing
├── metrics       # System metrics
├── resilience    # Retry, timeout & recovery logic
├── system        # Readiness & health checks
└── util          # Utility helpers
```

---

## ⚙️ Configuration

### Application Profiles

- `application.properties` → default (safe)
- `application-local.properties` → local development
- `application-prod.properties` → production

### Run Locally

```bash
SPRING_PROFILES_ACTIVE=local
```
SPRING_PROFILES_ACTIVE=local
```

### 🔐 Magic OTPs (Test/Dev)

To simplify testing without SMS integration, you can configure static OTPs via Environment Variables:

| Variable | Role | Description |
| :--- | :--- | :--- |
| `MAGIC_OTP_USER` | USER | Static OTP for any user logging in with `UserRole.PUBLIC` |
| `MAGIC_OTP_DRIVER` | DRIVER | Static OTP for any driver logging in with `UserRole.DRIVER` |

*If these are not set, a random OTP is generated.*

---

## 🧪 Tested Scenarios

- ✅ Multiple assignment retries
- ✅ Duplicate accept/reject calls
- ✅ Timeout-based auto reassignment
- ✅ Crash & restart recovery
- ✅ Concurrent ambulance updates
- ✅ Audit failures without system impact

---

## 🚀 Release Information

- **Current Version:** `v1.0.0`
- **Release Type:** Stable
- **Last Updated:** January 2026

---

## 🔒 License

This project is **proprietary** and intended for controlled deployment and evaluation only.

---

## 📬 Contact Me

I am a B.Tech CSE student passionate about building scalable Java applications.

[![Email](https://img.shields.io/badge/Email-anupamkushwaha639%40gmail.com-red?style=flat-square&logo=gmail)](mailto:anupamkushwaha639@gmail.com)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-anupamkushwaha85-blue?style=flat-square&logo=linkedin)](https://www.linkedin.com/in/anupamkushwaha85/)

---


### Built with ❤️ by Anupam Kushwaha

⭐ **If you find this project helpful, please give it a star!**
