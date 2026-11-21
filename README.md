# I-ITSM: Intelligent IT Service Management & Incident Resolution System

OOP course project (CS F213, Sem I 2026-27). A full-stack Java web application that manages the
complete lifecycle of IT incidents:

**Report → Classify → Prioritise → Assign → Investigate → Resolve → Verify → Close**

with SLA monitoring, notifications, audit, analytics, and AI-assisted features.

**Team:** Eshan Singh · Nishant Sharma · Mohit Samar

| | |
|---|---|
| Language | Java 21 |
| Backend | Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, Scheduling) |
| UI | Thymeleaf + Bootstrap 5 + Chart.js + vis-network (server-rendered, live updates via Server-Sent Events) |
| Database | PostgreSQL 17 (schema managed by Flyway); H2 for tests and a zero-install fallback |
| AI | Google Gemini API (optional) with an offline TF-IDF fallback |
| Tests | JUnit 5, AssertJ, Spring Boot Test, MockMvc: **67 automated tests** |
| API | REST + OpenAPI/Swagger UI at `/swagger-ui.html` |

---

## 1. Running it

### Prerequisites
* JDK 21 or newer (`java -version`)
* PostgreSQL 17, or use the H2 option below

Maven does **not** need to be installed: the `./mvnw` wrapper downloads it.

### Option A: PostgreSQL (default)
```bash
brew install postgresql@17 && brew services start postgresql@17
```
```bash
/opt/homebrew/opt/postgresql@17/bin/psql -d postgres -c "CREATE ROLE itsm LOGIN PASSWORD 'itsm';" -c "CREATE DATABASE itsm OWNER itsm;"
```
```bash
./mvnw spring-boot:run
```

### Option B: no database install (H2 file database)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

Open **http://localhost:8080**. On the first start with an empty database, demo data is created:
11 services with dependencies, SLA policies, users, 28 resolved historical incidents (these train the
classifier and the knowledge base) and 4 live incidents.

### Demo accounts (password `Password123` for all)

| Role | Email | Notes |
|---|---|---|
| Administrator | `admin@itsm.local` | IT Manager |
| Technician (team lead) | `arjun@itsm.local` | Network / Security / Cloud |
| Technician | `rahul@itsm.local` | Application / Database |
| Technician | `sneha@itsm.local`, `karan@itsm.local`, `ananya@itsm.local` | |
| Employee | `rohan@itsm.local`, `meera@itsm.local`, `vikram@itsm.local`, `aisha@itsm.local`, `dev@itsm.local` | |

To reset the demo, drop and recreate the database (or delete the `data/` folder for H2).

### Environment variables (all optional)

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection (default `jdbc:postgresql://localhost:5432/itsm`, `itsm`/`itsm`) |
| `GEMINI_API_KEY` | Enables the Gemini AI features (free key from https://aistudio.google.com) |
| `GEMINI_MODEL` | Default `gemini-3.8-flash` |
| `GEMINI_FALLBACK_MODELS` | Tried in order when the main model is overloaded (default `gemini-3.1-flash-lite-preview,gemini-flash-latest`). Each AI call is capped at 20 s; if every model fails, AI is skipped for 2 minutes and the offline algorithms answer instantly |
| `MAIL_ENABLED=true` + `SPRING_MAIL_HOST/PORT/USERNAME/PASSWORD` | Send real email (e.g. Gmail with an app password). Otherwise emails are simulated and visible in **Monitor → Outbox** |
| `SEED_DATA=false` | Start without demo data |

```bash
GEMINI_API_KEY=your-key ./mvnw spring-boot:run
```

### Tests
```bash
./mvnw test
```
Runs unit tests (`*Test`) and Spring integration tests (`*IT`) against in-memory H2. See
[docs/TEST_CASES.md](docs/TEST_CASES.md).

---

## 2. Feature map (brief → implementation)

### MVP
| Requirement | Where |
|---|---|
| 3 user types + register/login | `domain/user/*` (abstract `User` → `Employee`, `Technician`, `Administrator`), `/register`, Spring Security |
| Incident create/view/update/assign/resolve/close | `IncidentService`, `/incidents/**` |
| Priority from impact × urgency × criticality | `WeightedPriorityCalculator`, `MatrixPriorityCalculator` (switchable) |
| ≥2 assignment strategies | Skill-based, Load-based, Round-robin, Smart (weighted); switchable at runtime in **Settings** |
| SLA calculation, deadline tracking, violation detection, warnings | `SlaTracker`, `SlaService`, background `SlaMonitor` (every 30 s), `SlaViolation` records |
| ≥2 notification mechanisms | In-app bell, live browser push (SSE), Email (simulated or SMTP), SMS (simulated) |
| Complete audit history | `AuditListener` writes an `AuditEntry` for every event; per-incident timeline + global **Audit log** |
| ≥5 search criteria | 10: ID, keyword, status, priority, category, service, technician/unassigned, SLA status, date range, open-only (+ CSV export) |
| ≥2 intelligent features | See below (7 implemented) |
| Persistence | PostgreSQL + Flyway migration `V1__schema.sql` |

### Intelligent features
1. **Automatic categorisation**: Gemini AI when configured, otherwise a keyword + TF-IDF classifier that learns from past incidents.
2. **Duplicate detection**: TF-IDF cosine similarity, shown live while the employee types, with a "This affects me too" button that folds the report into the existing incident and raises its priority.
3. **Smart technician recommendation**: explainable weighted score (skills, capacity, service history, category history, seniority).
4. **Automatic escalation**: unacknowledged P1/P2 incidents go Technician → Team Lead → IT Manager.
5. **Predictive SLA risk**: risk score (0–100%) with reasons, for every open incident.
6. **Suggested resolutions**: knowledge-base search of similar fixed incidents, plus optional AI troubleshooting steps.
7. **Incident correlation**: uses the service dependency graph to flag probable root causes, knock-on effects, and outage clusters.

### Innovation extras
* **AI Quick Report**: describe the problem in plain words and get a pre-filled form (Gemini or heuristics).
* **Real-time operational dashboard**: Server-Sent Events push updates to all open dashboards and toast notifications.
* **Service dependency graph** visualisation with live health colours.
* **REST API** with OpenAPI docs, CSV export, Dockerfile, docker-compose and GitHub Actions CI.

---

## 3. Demo script (the three workflows required for the video)

**Workflow 1: report → prioritise → assign → notify**
1. Log in as `vikram@itsm.local` and open **Report incident**.
2. Title "Payment checkout failing", service *Payment Gateway*, impact *Organisation-wide*, urgency *Work blocked*, 300 users.
   While you type, the right-hand panel predicts **P1**, suggests the category and shows known fixes.
3. Submit. The banner says "created as P1 and assigned to Rahul Verma". Open the audit trail: CREATED → CLASSIFIED → PRIORITY_SET → ASSIGNED.
4. Log in as `rahul@itsm.local` in a second browser: a toast pops up live and the bell shows the assignment. Admin → **Outbox** shows the email/SMS.

**Workflow 2: investigate → SLA monitored → resolve → user notified**
1. As Rahul: open the incident, **Acknowledge** (response SLA met), **Start investigation**.
2. **Request information**: the SLA clock pauses (status *Awaiting info*).
3. As Vikram: answer the question; the clock resumes.
4. As Rahul: pick a **Suggested resolution** (or ask AI) and **Resolve**.
5. As Vikram: rate 5★ → *Verified* → **Close**. Show **Reports** and **SLA violations**.

**Workflow 3: multiple reports → duplicates / correlation / escalation**
1. As `rohan@itsm.local`, start reporting "Unable to make payment". The panel shows *"Someone may have reported this already: INC10xx"*. Click **This affects me too** (affected users rise, priority may rise).
2. Report another incident anyway; as admin, its detail page shows *Possible duplicate* → **Close as duplicate**.
3. Report two or three incidents on *Core Database* quickly, then a payment one: **Related incidents** shows *"Probable root cause … Payment Gateway depends on Core Database"* and a *Possible major outage* alert to admins.
4. Leave a P1 unacknowledged for more than 5 minutes: the monitor records a **response SLA breach** and **escalates to the team lead** (Arjun gets a live toast). The **Service dependency map** turns red.

Tip: in **Settings**, switch the assignment strategy (e.g. Round-robin) and report again to show the Strategy pattern live.

---

## 4. Project structure

```
src/main/java/com/itsm
├── domain/            ← business objects and rules (no Spring/web code)
│   ├── user/          ← User (abstract) → Employee, Technician, Administrator
│   ├── incident/      ← Incident aggregate, Comment, AuditEntry, enums
│   │   └── state/     ← State pattern: one class per lifecycle state
│   ├── priority/      ← Strategy: PriorityCalculator implementations
│   ├── assignment/    ← Strategy + Template Method: assignment strategies
│   ├── sla/           ← SlaPolicy, SlaTracker (embedded), SlaViolation
│   ├── catalog/       ← Category, ItService (dependency graph)
│   ├── event/         ← Observer: IncidentEvent hierarchy
│   └── notification/  ← Notification, OutboundMessage, ChannelType
├── service/           ← use cases: IncidentService, AssignmentService, SlaMonitor, …
├── intelligence/      ← classifiers, duplicate detector, correlation, risk, AI (Gemini adapter)
├── notification/      ← channels, Factory, Adapter (SMTP), listeners, live push
├── repository/        ← Repository pattern (Spring Data JPA)
├── security/          ← Spring Security configuration
├── web/               ← MVC controllers, forms, REST API
├── exception/         ← custom exception hierarchy
└── config/            ← bean wiring, demo data
```

Design documentation: [docs/DESIGN.md](docs/DESIGN.md) (UML, ER, patterns, algorithms) ·
[docs/architecture.md](docs/architecture.md) (decisions and rationale) · [docs/TEST_CASES.md](docs/TEST_CASES.md).
