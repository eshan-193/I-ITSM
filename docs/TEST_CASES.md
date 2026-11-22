# Test cases

All test cases are automated and run with `./mvnw test` (unit tests need no database; integration
tests start the full Spring application on an in-memory H2 database with the demo data). Each test's
`@DisplayName` starts with its ID, so the IDs below match the test report.

**Result of the last run: 68 tests executed (50 documented cases, some parameterised), 0 failures.**

Coverage by required category: normal operations, boundary conditions, invalid inputs, exceptions,
authorization, business rules, plus intelligent features and background processing.

| ID | Test case | Category | Level | Test class | Result |
|---|---|---|---|---|---|
| TC-01 | Full lifecycle NEW → … → CLOSED follows the defined order | Normal | Unit | `IncidentLifecycleTest` | Pass |
| TC-02 | NEW → CLOSED directly is rejected | Invalid transition | Unit | `IncidentLifecycleTest` | Pass |
| TC-03 | Cannot acknowledge before a technician is assigned | Invalid transition | Unit | `IncidentLifecycleTest` | Pass |
| TC-04 | Resolving requires resolution details | Invalid input | Unit | `IncidentLifecycleTest` | Pass |
| TC-05 | Reporter rejecting a fix reopens the incident and counts the reopen | Business rule | Unit | `IncidentLifecycleTest` | Pass |
| TC-06 | Feedback rating must be 1–5 (boundary values) | Boundary | Unit | `IncidentLifecycleTest` | Pass |
| TC-07 | Invalid incident input is rejected at construction | Invalid input | Unit | `IncidentLifecycleTest` | Pass |
| TC-08 | Closing as duplicate links to the original and is only allowed before work starts | Business rule | Unit | `IncidentLifecycleTest` | Pass |
| TC-09 | 'Me too' adds a watcher and increases affected users once | Business rule | Unit | `IncidentLifecycleTest` | Pass |
| TC-10 | Every state's advertised actions match the transitions it really allows | Business rule (all states × actions) | Unit | `IncidentLifecycleTest` | Pass |
| TC-11 | Weighted score boundaries map to the documented priority bands | Boundary | Unit | `PriorityCalculatorTest` | Pass |
| TC-12 | More than 50 affected users raises priority one level (never above P1) | Boundary | Unit | `PriorityCalculatorTest` | Pass |
| TC-13 | ITIL matrix adjusts for business criticality | Business rule | Unit | `PriorityCalculatorTest` | Pass |
| TC-14 | Skill-based: only skilled technicians, least loaded first; full technicians excluded | Normal / business rule | Unit | `AssignmentStrategyTest` | Pass |
| TC-15 | Load-based: lowest workload ratio regardless of skill | Normal | Unit | `AssignmentStrategyTest` | Pass |
| TC-16 | Round-robin: consecutive incidents go to different technicians | Normal | Unit | `AssignmentStrategyTest` | Pass |
| TC-17 | Smart: an experienced, skilled technician with history beats an idle unskilled one | Intelligent feature | Unit | `AssignmentStrategyTest` | Pass |
| TC-18 | No eligible technician (all unavailable or full) → empty result | Exception: no technician | Unit | `AssignmentStrategyTest` | Pass |
| TC-19 | Deadlines are computed from the policy | Normal | Unit | `SlaTrackerTest` | Pass |
| TC-20 | Breaches are detected exactly once, only after the deadline | Boundary | Unit | `SlaTrackerTest` | Pass |
| TC-21 | Waiting for the reporter pauses the clock and pushes the deadline back | Business rule | Unit | `SlaTrackerTest` | Pass |
| TC-22 | Warning fires at the configured threshold, once | Boundary | Unit | `SlaTrackerTest` | Pass |
| TC-23 | Invalid SLA policy values are rejected | Invalid input | Unit | `SlaTrackerTest` | Pass |
| TC-24 | Role permissions: employee own only, technician assigned only, team lead and admin everything | Authorization | Unit | `UserPermissionTest` | Pass |
| TC-25 | Technician workload limit is enforced and validated | Boundary / invalid input | Unit | `UserPermissionTest` | Pass |
| TC-26 | Duplicate detection groups the brief's payment examples and ignores unrelated incidents | Intelligent feature | Unit | `IntelligenceTest` | Pass |
| TC-27 | Text analysis stems words and drops stop words | Intelligent feature | Unit | `IntelligenceTest` | Pass |
| TC-28 | Keyword classifier picks the category with the strongest evidence | Intelligent feature | Unit | `IntelligenceTest` | Pass |
| TC-29 | SLA risk: fresh incident is low risk; unassigned and nearly due is high risk | Intelligent feature | Unit | `IntelligenceTest` | Pass |
| TC-30 | Unacknowledged P1 escalates after the response target, then again one window later | Intelligent feature / boundary | Unit | `EscalationRuleTest` | Pass |
| TC-31 | P3/P4 incidents are never auto-escalated | Business rule | Unit | `EscalationRuleTest` | Pass |
| TC-32 | Reporting runs the full pipeline: classify, prioritise, SLA, assign, audit, notify | Normal (end-to-end) | Integration | `IncidentServiceIT` | Pass |
| TC-33 | Reporting the same problem twice is blocked unless the reporter confirms it is different | Exception: duplicate incident | Integration | `IncidentServiceIT` | Pass |
| TC-34 | Employees cannot do technician work or open other people's incidents | Authorization | Integration | `IncidentServiceIT` | Pass |
| TC-35 | Manual assignment respects workload limits unless explicitly overridden | Exception: workload exceeded | Integration | `IncidentServiceIT` | Pass |
| TC-36 | Unavailable technician cannot be assigned | Exception: technician unavailable | Integration | `IncidentServiceIT` | Pass |
| TC-37 | Background SLA monitor records breaches and escalates an unacknowledged P1 | Background processing | Integration | `IncidentServiceIT` | Pass |
| TC-38 | Missing SLA configuration is reported clearly | Exception: missing SLA config | Integration | `IncidentServiceIT` | Pass |
| TC-39 | Search filters by priority and scopes employees to their own incidents | Normal / authorization / invalid input | Integration | `IncidentServiceIT` | Pass |
| TC-40 | Switching the assignment strategy at runtime changes who is chosen; invalid settings rejected | Business rule / invalid input | Integration | `IncidentServiceIT` | Pass |
| TC-41 | Registration validates password strength and unique email | Invalid input | Integration | `IncidentServiceIT` | Pass |
| TC-42 | Circular service dependencies are rejected | Invalid input | Integration | `IncidentServiceIT` | Pass |
| TC-43 | 'Me too' raises affected users and can raise the priority | Intelligent feature | Integration | `IncidentServiceIT` | Pass |
| TC-44 | Anonymous browsers are sent to the login page; anonymous API calls get 401 | Authorization | Integration | `WebSecurityIT` | Pass |
| TC-45 | Invalid login is rejected | Exception: invalid login | Integration | `WebSecurityIT` | Pass |
| TC-46 | Employees cannot open administrator pages | Authorization | Integration | `WebSecurityIT` | Pass |
| TC-47 | Each role gets its own home page | Normal | Integration | `WebSecurityIT` | Pass |
| TC-48 | REST API returns problem JSON for missing incidents and validates input | Exception: not found / invalid input | Integration | `WebSecurityIT` | Pass |
| TC-49 | Live analysis API suggests category and predicts priority | Intelligent feature | Integration | `WebSecurityIT` | Pass |
| TC-50 | A reporter's reply in the conversation answers an information request and resumes the SLA | Business rule | Integration | `IncidentServiceIT` | Pass |

## Exception coverage

| Required situation | Exception / behaviour | Test |
|---|---|---|
| Invalid login | Redirect to `/login?error` | TC-45 |
| Unauthorized operation | `UnauthorizedOperationException` (403) | TC-24, TC-34, TC-46 |
| Invalid incident | `ValidationException` (400) | TC-07, TC-48 |
| Invalid state transition | `InvalidStateTransitionException` (409) | TC-02, TC-03, TC-10 |
| Technician unavailable | `TechnicianUnavailableException` | TC-18, TC-36 |
| Technician workload exceeded | `WorkloadExceededException` | TC-35 |
| Service not found | `ResourceNotFoundException` (404) | TC-48 |
| Missing SLA configuration | `SlaConfigurationMissingException` | TC-38 |
| Duplicate incident | `DuplicateIncidentException` | TC-33 |
| Database failure | `DataAccessException` → friendly 503 page (`WebExceptionHandler`) | manual |
| Network failure (AI provider) | `ExternalServiceException` → automatic offline fallback | manual: unset/invalid `GEMINI_API_KEY` |
| Invalid user input | Bean Validation messages on forms | TC-41, TC-48, manual (form) |

## Manual / exploratory checks performed
* All pages rendered for each role (crawl of 37 URLs): no server errors.
* The three demo workflows were executed over HTTP against PostgreSQL: report → P1 → assigned → notified;
  acknowledge → request info (SLA paused) → provide info → resolve → verify → close; duplicate detection,
  "me too", close-as-duplicate.
* Live analysis panel, dependency graph and live dashboard were checked in a browser.
