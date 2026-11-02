# OpenControl ERP

> **The first truly open-source enterprise ERP designed for modular extensibility and complex statutory compliance.**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![JVM](https://img.shields.io/badge/JVM-21%20LTS-orange.svg)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-green.svg)]()
[![Coverage](https://img.shields.io/badge/Coverage-%E2%89%A5%2090%25-brightgreen.svg)]()

---

## 🏛️ Vision & Architectural Philosophy

OpenControl ERP is built from the ground up to solve the real-world friction of enterprise resource planning. Most "open source" ERPs are either stripped-down open-core software or poorly architected monolithic balls of mud.

OpenControl ERP is governed by strict [Constitutional Principles](.specify/memory/constitution.md):
1. **Modular Monolith & Clean Architecture**: High cohesion, low coupling, no cross-module database joins, and zero premature microservices complexity.
2. **First-Class Plugin & App Engine**: Pluggable capabilities via Service Provider Interfaces (SPIs) and isolated classloaders without modifying core code.
3. **Brazil-First Localization with Multi-Jurisdiction Abstraction**: Deep support for Brazilian tax and electronic invoicing (NF-e, NFS-e, CT-e, MDF-e, SPED) via proven Java DFe libraries (e.g. Samuel Oliveira ecosystem), abstracted behind a generic `FiscalEngineSPI` ready for global expansion.
4. **Codebase Standards**: Strict English for all code, domain models, APIs, and schemas, preserving official jurisdiction acronyms where legally required.
5. **Modern Componentized UI**: Modern React, TypeScript, Tailwind CSS, and shadcn/ui with pluggable frontend extension slots.
6. **Transactional Integrity & Financial Immutability**: Append-only double-entry bookkeeping ledger and immutable audit trails.
7. **Minimum 90% Automated Test Coverage Gate (Non-Negotiable)**: JaCoCo build verification gate strictly failing any build below 90%.
8. **Mandatory Documentation for Every Feature (Non-Negotiable)**: Every feature must have complete Spec Kit design artifacts, OpenAPI contracts, architecture manuals, and KDoc.

---

## 🛠️ Technology Stack

- **Language & Runtime**: Kotlin 2.x on JVM 21 LTS (100% interoperable with Java)
- **Framework**: Spring Boot 4.1.2, Spring Modulith, Spring Data JPA
- **Database & Migrations**: PostgreSQL 16+ versioned with Flyway
- **Build Tool**: Gradle (Kotlin DSL `build.gradle.kts`)
- **Code Standards & Coverage**: Spotless (ktlint) + JaCoCo (90% minimum threshold)
- **API Documentation**: OpenAPI 3.0 / Swagger UI (SpringDoc)

---

## 📂 Repository Structure

```text
OpenControlErp/
├── .specify/                  # Spec Kit governance & canonical Constitution
├── specs/                     # Feature specifications & technical plans
│   └── 001-extension-engine/  # Feature 001: Extension Engine & Event Bus
├── docs/                      # Architectural & developer manuals
│   └── architecture/          # In-depth architectural guides
└── backend/                   # Kotlin & Spring Boot Backend Kernel
    ├── build.gradle.kts       # Gradle build configuration
    ├── src/main/kotlin/       # Kernel source code (plugin, spi, event, api)
    ├── src/main/resources/    # Application configuration & Flyway migrations
    └── src/test/kotlin/       # Unit & Integration tests (MockK, Testcontainers)
```

---

## 🚀 Quick Start

### 1. Prerequisites
- **JDK 21** installed (`java -version`)
- **Docker** (optional, for local PostgreSQL or Testcontainers)

### 2. Run Tests & Coverage Verification
```bash
cd backend
./gradlew check
```
*(Runs all unit and integration tests and validates that code coverage is >= 90%)*

### 3. Run the Backend Locally
```bash
cd backend
./gradlew bootRun
```

Access Swagger UI documentation at:  
👉 `http://localhost:8080/swagger-ui.html`

---

## 📖 Feature 001: Extension Engine Documentation

The complete architectural guide and developer tutorial for building plugins is available at:  
📄 **[Developer Guide: Extension Engine & Plugin Architecture](docs/architecture/extension-engine.md)**
