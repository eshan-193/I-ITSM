<div class="cover">

<div class="course">CS F213 · Object-Oriented Programming · Semester I, 2026–27</div>

# I-ITSM
## Intelligent IT Service Management &amp; Incident Resolution System

<div class="subtitle">Project Report</div>

<table class="members">
<tr><th>Group</th><td colspan="2">{{GROUP}}</td></tr>
<tr><th>Eshan Singh</th><td>2025A7PS0109H</td><td>Architecture, domain model, OOP &amp; design patterns</td></tr>
<tr><th>Nishant Sharma</th><td>2025A7PS0154H</td><td>Database &amp; persistence, lifecycle services, SLA, notifications, security</td></tr>
<tr><th>Mohit Samar</th><td>2025A7PS0921H</td><td>Intelligent features, search &amp; reporting, admin UI, testing</td></tr>
</table>

<div class="repo">Source code: <a href="https://github.com/eshan-193/I-ITSM">github.com/eshan-193/I-ITSM</a></div>

</div>

<div class="toc">

## Contents

1. Problem Analysis
2. Requirements Specification
3. System Architecture
4. UML Diagrams
5. Object-Oriented Design and Design Patterns
6. Intelligent Features and Algorithms
7. Technology Stack and Selection
8. Database Design
9. Security
10. Testing
11. Source Code and Running the System
12. Team Responsibilities
13. Limitations and Future Work

</div>

# 1. Problem Analysis

{{DESIGN:1}}

**Existing challenges addressed.** Delayed response, incorrect prioritisation, inefficient technician
allocation, SLA violations noticed too late, poor visibility into service health, duplicate reports of the
same problem, incomplete incident history, and difficulty spotting recurring problems. Each is mapped to
a feature below:

| Challenge | How I-ITSM addresses it |
|---|---|
| Delayed response | Automatic classification, prioritisation and assignment the moment an incident is reported; live notifications |
| Incorrect prioritisation | Documented priority algorithm (Impact × Urgency × Business criticality, adjusted for affected users) |
| Inefficient technician allocation | Four interchangeable assignment strategies that consider skills, workload, history and experience |
| SLA violations | Deadlines per priority and service, a background monitor, warnings at 75 %, breach records, automatic escalation |
| Poor service visibility | Operations dashboard, service health and a service dependency map |
| Duplicate reports | Text-similarity duplicate detection while typing, "This affects me too", close-as-duplicate |
| Incomplete history | Audit entry for every action with user, timestamp, previous and new state |
| Recurring problems | Reports by service and category, incident correlation, suggested resolutions from past fixes |

# 2. Requirements Specification

## User roles

| Role | Main capabilities |
|---|---|
| **Employee** | Register and log in; report incidents (form or plain-language quick report); view and track own incidents; add information; answer technicians' questions; verify or reopen a resolution; give feedback; join an existing incident ("me too") |
| **Technician** | View assigned queue and workload; acknowledge; investigate and add notes; update status; request information; resolve with resolution and root cause; triage (re-classify, re-prioritise); close duplicates |
| **Team Lead** (a technician with a flag) | Everything a technician can do on any incident; first level of escalation; assign incidents |
| **Administrator / IT Manager** | Manage users, technicians, IT services, categories and SLA policies; choose assignment and priority strategies; monitor incidents; view reports, audit logs, SLA violations and outbox; final level of escalation |

{{DESIGN:2}}

# 3. System Architecture

{{DESIGN:3}}

| Layer | Responsibility | Example |
|---|---|---|
| **Presentation** (`web`) | Receive HTTP requests, validate form input, choose the page | `IncidentController`, `ReportIncidentForm` |
| **Service** (`service`) | Coordinate a use case inside a transaction | `IncidentService.report()` classifies, prioritises, assigns, saves and publishes events |
| **Domain** (`domain`) | Business objects and rules; no web or SQL code | `Incident.acknowledge()` refuses illegal transitions |
| **Repository** (`repository`) | Load and save objects | `IncidentRepository` (Spring Data JPA) |
| **Cross-cutting** | Security, events, schedulers, intelligence | `SecurityConfig`, `AuditListener`, `SlaMonitor`, `DuplicateDetector` |

**Why layers?** Each class has one kind of job, so it is easier to find, change and test. Dependencies
point downward only, which keeps the domain independent of the web and the database and testable
without starting a server. It also let the team work in parallel against agreed method signatures.

# 4. UML Diagrams

{{DESIGN:4}}

# 5. Object-Oriented Design and Design Patterns

{{DESIGN:5}}

## Exception hierarchy

```
ItsmException (abstract, unchecked, declares httpStatus())
├── InvalidStateTransitionException   409
├── UnauthorizedOperationException    403
├── ResourceNotFoundException         404
├── ValidationException               400
├── TechnicianUnavailableException
├── WorkloadExceededException
├── SlaConfigurationMissingException
├── DuplicateIncidentException
└── ExternalServiceException          (AI provider / network failure → offline fallback)
```

One common parent lets a single `@ControllerAdvice` handler (and one for the REST API) turn any
business-rule violation into a clear message, while code that needs special handling can still catch a
specific subtype. The exceptions are unchecked because the caller usually cannot recover locally;
checked exceptions would force `throws` clauses through every layer.

# 6. Intelligent Features and Algorithms

Seven intelligent or automated features are implemented (the brief requires two):

| # | Feature | Approach |
|---|---|---|
| 1 | Automatic categorisation | Keyword + TF-IDF classifier that learns from past incidents; Google Gemini when an API key is configured |
| 2 | Duplicate detection | TF-IDF cosine similarity shown live while typing; "This affects me too" folds the report into the existing incident |
| 3 | Smart technician recommendation | Explainable weighted score over skills, capacity, service and category history, and experience |
| 4 | Automatic escalation | Unacknowledged P1/P2 incidents escalate Technician → Team Lead → IT Manager |
| 5 | Predictive SLA risk | Explainable 0–100 risk score with reasons for every open incident |
| 6 | Suggested resolutions | Knowledge-base search over similar resolved incidents, plus optional AI troubleshooting steps |
| 7 | Incident correlation | Service dependency graph flags probable root causes, knock-on effects and outage clusters |

{{DESIGN:6}}

# 7. Technology Stack and Selection

## Technologies used

| Concern | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Backend framework | Spring Boot (Web MVC, Data JPA, Security, Validation, Mail, Actuator, Scheduling) | 4.1.2 |
| Views | Thymeleaf (+ Spring Security extras), server-rendered | — |
| UI styling and charts | Bootstrap 5.3, Bootstrap Icons, Chart.js 4.4, vis-network (dependency graph) | — |
| Live updates | Server-Sent Events | — |
| Database | PostgreSQL (H2 for automated tests and as a zero-install fallback) | 17 |
| ORM and migrations | Hibernate via Spring Data JPA; Flyway schema migrations | — |
| API documentation | springdoc-openapi (Swagger UI) | — |
| AI (optional) | Google Gemini REST API, behind an adapter, with offline fallback | — |
| Build | Maven (via the Maven Wrapper) | 3.10 |
| Testing | JUnit 5, AssertJ, Spring Boot Test, MockMvc | — |
| Delivery | Git + GitHub, GitHub Actions CI, Dockerfile and docker-compose | — |

## Selection rationale

**1. Why Java?** It is the course language and the whole team already knew it, so every member can read
and defend every part of the code. Its static typing, interfaces and mature tooling suit an
object-oriented design.

**2. Why this frontend technology?** Server-rendered Thymeleaf pages styled with Bootstrap. A Thymeleaf
template is plain HTML with a few attributes, matching the team's HTML/CSS skills, and it keeps the whole
application in one language. Each role gets its own navigation, home page and colour theme.

**3. Why Spring Boot for the backend?** It provides request routing, dependency injection, transactions,
authentication, scheduling and email out of the box, so the team's time went into the graded parts:
the domain model, patterns and features. Dependency injection encourages programming to interfaces.

**4. Why PostgreSQL?** The data is strongly relational (an incident belongs to a reporter, is assigned to
a technician, affects a service and has many audit entries). A relational database with foreign keys and
check constraints protects that consistency. H2 runs the automated tests in memory without a server.

**5. Why this architecture?** A layered monolith is simple to build, test, deploy and explain, gives a
clear separation of concerns, and was the right size for a three-person team on a short timeline.

**6. Alternatives considered.**
React or Angular + REST API (rejected: a second language and much new tooling before any feature could
be built); JavaFX desktop app (rejected: awkward for many users sharing one database, dated look);
plain servlets and JDBC (rejected: re-implementing what Spring provides); MongoDB (rejected: our data is
relational); microservices (rejected: deployment and data-consistency overhead with no benefit at our scale).

**7. Advantages and limitations.** Advantages: one language end to end; strong security and transaction
support; the domain is framework-independent and unit-testable; database portability through JPA;
mature ecosystem. Limitations: server-rendered pages are less interactive than a single-page app
(mitigated by small JavaScript enhancements and Server-Sent Events); Spring's startup time and memory
use are higher than lighter frameworks; the monolith scales as one unit.

**8. How the technology supports object-oriented design.** Business logic lives in plain Java classes.
Spring wires objects together through constructor injection, so classes depend on interfaces
(`AssignmentStrategy`, `NotificationChannel`, `PriorityCalculator`) and Spring supplies all
implementations; JPA maps inheritance (`User` hierarchy), composition (embedded `SlaTracker`, cascaded
comments and audit entries) and associations directly; Spring's event system implements Observer.

**9. Evolution with many more users.**
{{DESIGN:9}}

# 8. Database Design

{{DESIGN:7}}

## Main tables

| Table | Purpose | Important relationships |
|---|---|---|
| `users` | All users, single-table inheritance with a `role` discriminator | Technician columns (`max_active_incidents`, `available`, `team_lead`) used only for technicians |
| `technician_skills` | Technician ↔ category join table | Many-to-many |
| `it_services`, `service_dependencies` | IT services with business criticality, and the dependency graph | Self-referencing many-to-many |
| `categories` | Incident categories with keywords for classification | Managed by admins as data |
| `sla_policies` | Response and resolution targets per priority, optionally per service | `service_id` NULL = default policy |
| `incidents` | The incident, including the embedded SLA tracker columns | FK to reporter, assignee, service, category, duplicate_of |
| `audit_entries`, `comments`, `sla_violations` | Owned by an incident | FK to incident (composition) |
| `notifications`, `outbound_messages` | In-app notifications and the email/SMS outbox | FK to user and incident |
| `system_settings` | Key/value settings such as the active strategy | — |

# 9. Security

{{DESIGN:8}}

# 10. Testing

{{TESTS}}

# 11. Source Code and Running the System

The complete source code is on GitHub: **https://github.com/eshan-193/I-ITSM**

```
src/main/java/com/itsm
├── domain/        business objects and rules (no Spring/web code)
│   ├── user/        User (abstract) → Employee, Technician, Administrator
│   ├── incident/    Incident aggregate, Comment, AuditEntry; state/ = State pattern
│   ├── priority/    PriorityCalculator strategies
│   ├── assignment/  AssignmentStrategy strategies + Template Method base
│   ├── sla/         SlaPolicy, SlaTracker, SlaViolation
│   ├── catalog/     Category, ItService (dependency graph)
│   └── event/       IncidentEvent hierarchy (Observer)
├── service/       use cases, schedulers, search
├── intelligence/  classifiers, duplicates, correlation, SLA risk, AI adapter
├── notification/  channels, factory, adapters, listeners, live push
├── repository/    Spring Data JPA repositories
├── security/      Spring Security configuration
├── web/           MVC controllers, forms, REST API
├── exception/     custom exception hierarchy
└── config/        bean wiring, demo data
```

**Running it.** Requires JDK 21+ and PostgreSQL 17 (or the H2 profile, no install needed):

```
./mvnw spring-boot:run                                    # PostgreSQL
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2      # H2 file database
./mvnw test                                               # automated tests
```

Open http://localhost:8080. Demo accounts (password `Password123`): `admin@itsm.local` (administrator),
`arjun@itsm.local` (technician, team lead), `rahul@itsm.local` (technician), `rohan@itsm.local` (employee).

# 12. Team Responsibilities

| Member | ID | Responsibilities |
|---|---|---|
| **Eshan Singh** | 2025A7PS0109H | System architecture and technology selection; domain model (`Incident`, `User` hierarchy); State pattern lifecycle; Strategy patterns for priority and assignment; class and architecture diagrams |
| **Nishant Sharma** | 2025A7PS0154H | Database design, schema and Flyway migration; repositories and search specifications; incident lifecycle services; SLA tracking and background monitor; notifications (Observer, Factory, Adapter) and audit; Spring Security; ER diagram |
| **Mohit Samar** | 2025A7PS0921H | Intelligent features (duplicate detection, classification, correlation, SLA risk, suggested resolutions, escalation); search, dashboards and reports; administrator configuration pages; automated test suite and test-case documentation |

Every member reviewed the full design and can explain the overall system.

# 13. Limitations and Future Work

- **SLA calendar.** Clocks run 24×7; a business-hours and holiday calendar would make targets fairer.
- **Notifications.** Email and SMS are simulated by default (visible in the Outbox); real SMTP is supported through configuration, and a real SMS gateway only needs a new adapter.
- **AI.** Gemini features need an API key; without one the offline algorithms answer instantly, so the system never depends on the network.
- **Security hardening.** Account lockout, multi-factor authentication and tamper-evident audit logs are documented as future work.
- **Scale.** The evolution path in Section 7 (load-balanced instances, message broker, asynchronous workers, read replicas, splitting notifications and analytics into services) is supported by the event-based, interface-driven design.
