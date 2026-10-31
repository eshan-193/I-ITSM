package com.itsm.config;

import com.itsm.domain.catalog.BusinessCriticality;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.*;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.priority.PriorityResult;
import com.itsm.domain.sla.SlaBreachType;
import com.itsm.domain.sla.SlaPolicy;
import com.itsm.domain.sla.SlaViolation;
import com.itsm.domain.user.Administrator;
import com.itsm.domain.user.Employee;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.repository.*;
import com.itsm.service.PriorityService;
import com.itsm.service.SlaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Fills an empty database with a realistic organisation: users for every role, IT
 * services with dependencies, SLA policies, two weeks of resolved incident history
 * (which trains the classifier and the knowledge base) and a handful of live incidents.
 * <p>
 * Runs only when {@code itsm.seed.enabled=true} and the users table is empty.
 * Every demo account uses the password {@value #DEMO_PASSWORD}.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "Password123";
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final boolean enabled;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ItServiceRepository services;
    private final SlaPolicyRepository slaPolicies;
    private final IncidentRepository incidents;
    private final AuditEntryRepository audit;
    private final SlaViolationRepository violations;
    private final PriorityService priorityService;
    private final SlaService slaService;
    private final PasswordEncoder encoder;
    private final TransactionTemplate tx;
    private final Clock clock;

    private final Map<String, Category> cat = new HashMap<>();
    private final Map<String, ItService> svc = new HashMap<>();
    private final Map<String, User> u = new HashMap<>();
    private LocalDateTime now;

    public DemoDataSeeder(@Value("${itsm.seed.enabled:true}") boolean enabled, UserRepository users,
                          CategoryRepository categories, ItServiceRepository services, SlaPolicyRepository slaPolicies,
                          IncidentRepository incidents, AuditEntryRepository audit, SlaViolationRepository violations,
                          PriorityService priorityService, SlaService slaService, PasswordEncoder encoder,
                          TransactionTemplate tx, Clock clock) {
        this.enabled = enabled;
        this.users = users;
        this.categories = categories;
        this.services = services;
        this.slaPolicies = slaPolicies;
        this.incidents = incidents;
        this.audit = audit;
        this.violations = violations;
        this.priorityService = priorityService;
        this.slaService = slaService;
        this.encoder = encoder;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || users.count() > 0) {
            return;
        }
        log.info("Empty database: seeding demo data (password for all accounts: {})", DEMO_PASSWORD);
        tx.executeWithoutResult(status -> {
            now = LocalDateTime.now(clock).withSecond(0).withNano(0);
            seedCategories();
            seedServices();
            seedSlaPolicies();
            seedUsers();
            seedHistory();
            seedLiveIncidents();
        });
        log.info("Demo data ready: {} users, {} services, {} incidents", users.count(), services.count(), incidents.count());
    }

    // ------------------------------------------------------------------ catalogue

    private void seedCategories() {
        category("HARDWARE", "Hardware", "Laptops, desktops, printers, peripherals",
                "laptop, desktop, printer, monitor, keyboard, mouse, screen, battery, power, charger, hardware, device, docking");
        category("SOFTWARE", "Software", "Desktop applications and installations",
                "install, software, excel, word, office, update, crash, license, application crash, outlook, teams");
        category("NETWORK", "Network", "LAN, Wi-Fi, VPN and internet connectivity",
                "network, wifi, wi-fi, vpn, internet, connection, disconnect, latency, router, switch, firewall, bandwidth, ethernet");
        category("DATABASE", "Database", "Database servers, queries and replication",
                "database, db, sql, query, replication, deadlock, index, table, postgres, oracle, cpu, slow query");
        category("CLOUD", "Cloud", "Cloud infrastructure and virtual machines",
                "cloud, vm, virtual machine, aws, azure, instance, kubernetes, container, cluster, security group");
        category("SECURITY", "Security", "Malware, phishing and security alerts",
                "phishing, virus, malware, antivirus, suspicious, breach, hacked, ransomware, spam, security");
        category("AUTHENTICATION", "Authentication", "Login, SSO, passwords and MFA",
                "login, log in, password, sso, single sign-on, mfa, otp, locked, account, authentication, credentials, sign in");
        category("APPLICATION", "Business Application", "Business systems such as Payments, CRM and HR portal",
                "payment, checkout, transaction, portal, crm, error 500, 502, page, dashboard, refund, invoice, billing");
        category("STORAGE", "Storage", "File shares, cloud drives and backups",
                "storage, drive, shared drive, file, folder, sync, upload, backup, disk, quota");
        category("EMAIL", "Email & Collaboration", "Email delivery, mailboxes and calendars",
                "email, mail, mailbox, outbox, inbox, calendar, meeting invite, smtp, attachment");
    }

    private void category(String code, String name, String description, String keywords) {
        cat.put(code, categories.save(new Category(code, name, description, keywords)));
    }

    private void seedServices() {
        service("Corporate Network", "Office LAN, Wi-Fi and internet uplink", BusinessCriticality.HIGH, "NETWORK");
        service("Core Database", "Primary PostgreSQL cluster used by business applications", BusinessCriticality.HIGH, "DATABASE");
        service("Single Sign-On", "Company identity provider (SSO and MFA)", BusinessCriticality.HIGH, "AUTHENTICATION");
        service("Payment Gateway", "Customer card payments, refunds and checkout", BusinessCriticality.HIGH, "APPLICATION");
        service("Corporate VPN", "Remote access for staff working from home", BusinessCriticality.MEDIUM, "NETWORK");
        service("Email & Calendar", "Company email, calendars and meeting invites", BusinessCriticality.MEDIUM, "EMAIL");
        service("Cloud Storage", "Shared drives and file sync", BusinessCriticality.MEDIUM, "STORAGE");
        service("CRM", "Customer relationship management for sales", BusinessCriticality.MEDIUM, "APPLICATION");
        service("Cloud Platform", "Virtual machines and containers hosting internal apps", BusinessCriticality.HIGH, "CLOUD");
        service("HR Portal", "Leave, payroll and employee self-service", BusinessCriticality.LOW, "APPLICATION");
        service("End-User Devices", "Laptops, desktops, printers and peripherals", BusinessCriticality.LOW, "HARDWARE");

        depends("Core Database", "Cloud Platform");
        depends("Single Sign-On", "Core Database");
        depends("Payment Gateway", "Core Database", "Single Sign-On", "Corporate Network");
        depends("Corporate VPN", "Corporate Network");
        depends("Email & Calendar", "Single Sign-On", "Corporate Network");
        depends("Cloud Storage", "Corporate Network", "Single Sign-On");
        depends("CRM", "Core Database", "Single Sign-On");
        depends("HR Portal", "Core Database", "Single Sign-On");
    }

    private void service(String name, String description, BusinessCriticality criticality, String defaultCategory) {
        svc.put(name, services.save(new ItService(name, description, criticality, cat.get(defaultCategory))));
    }

    private void depends(String service, String... dependencies) {
        for (String d : dependencies) {
            svc.get(service).addDependency(svc.get(d));
        }
    }

    private void seedSlaPolicies() {
        slaPolicies.save(new SlaPolicy(Priority.P1, null, 5, 30));
        slaPolicies.save(new SlaPolicy(Priority.P2, null, 15, 120));
        slaPolicies.save(new SlaPolicy(Priority.P3, null, 60, 480));
        slaPolicies.save(new SlaPolicy(Priority.P4, null, 240, 1440));
        // Payments are revenue-critical: tighter P2 target than the default.
        slaPolicies.save(new SlaPolicy(Priority.P2, svc.get("Payment Gateway"), 10, 60));
    }

    // ------------------------------------------------------------------ users

    private void seedUsers() {
        String hash = encoder.encode(DEMO_PASSWORD);

        Administrator admin = new Administrator("Priya Sharma", "admin@itsm.local", hash, "IT Manager");
        admin.updateProfile("Priya Sharma", "+91-90000-00001");
        u.put("admin", users.save(admin));

        u.put("arjun", technician("Arjun Mehta", "arjun@itsm.local", hash, 6, 9, true, "+91-90000-00011", "NETWORK", "SECURITY", "CLOUD"));
        u.put("rahul", technician("Rahul Verma", "rahul@itsm.local", hash, 5, 6, false, "+91-90000-00012", "APPLICATION", "DATABASE"));
        u.put("sneha", technician("Sneha Iyer", "sneha@itsm.local", hash, 5, 4, false, "+91-90000-00013", "AUTHENTICATION", "SECURITY", "EMAIL"));
        u.put("karan", technician("Karan Patel", "karan@itsm.local", hash, 6, 2, false, "+91-90000-00014", "HARDWARE", "SOFTWARE", "NETWORK"));
        u.put("ananya", technician("Ananya Rao", "ananya@itsm.local", hash, 5, 5, false, "+91-90000-00015", "CLOUD", "STORAGE", "DATABASE"));

        u.put("rohan", users.save(new Employee("Rohan Gupta", "rohan@itsm.local", hash, "Finance")));
        u.put("meera", users.save(new Employee("Meera Nair", "meera@itsm.local", hash, "Human Resources")));
        u.put("vikram", users.save(new Employee("Vikram Joshi", "vikram@itsm.local", hash, "Sales")));
        u.put("aisha", users.save(new Employee("Aisha Khan", "aisha@itsm.local", hash, "Engineering")));
        u.put("dev", users.save(new Employee("Dev Malhotra", "dev@itsm.local", hash, "Operations")));
    }

    private Technician technician(String name, String email, String hash, int max, int years, boolean lead,
                                  String phone, String... skills) {
        Technician t = new Technician(name, email, hash, max, years);
        t.setTeamLead(lead);
        t.updateProfile(name, phone);
        for (String s : skills) {
            t.addSkill(cat.get(s));
        }
        return users.save(t);
    }

    // ------------------------------------------------------------------ incident history

    private void seedHistory() {
        h("Payment page timing out at checkout", "Customers get a gateway timeout when paying by card. Started around 10am, affects every customer.",
                "Payment Gateway", "APPLICATION", Impact.HIGH, Urgency.HIGH, 200, "rohan", "rahul", 13, 4, 26,
                "Payment API connection pool was exhausted at peak load. Increased pool size from 20 to 50 and restarted the payment-api pods.",
                "Connection pool exhaustion under peak load", 5);
        h("Card transactions failing with error 502", "All card payments return 502 Bad Gateway since this morning.",
                "Payment Gateway", "APPLICATION", Impact.HIGH, Urgency.HIGH, 150, "vikram", "rahul", 9, 3, 22,
                "TLS certificate on the payment load balancer had expired. Renewed the certificate and enabled automatic renewal.",
                "Expired TLS certificate", 5);
        h("Refunds not processing", "Refund requests submitted yesterday are still pending, finance cannot close the day.",
                "Payment Gateway", "APPLICATION", Impact.MEDIUM, Urgency.MEDIUM, 6, "rohan", "rahul", 7, 20, 95,
                "Refund batch job was blocked by a locked row in the transactions table. Killed the blocking session and re-ran the batch.",
                "Database row lock", 4);
        h("Database queries extremely slow", "Reports and screens that read from the main database take over a minute to load.",
                "Core Database", "DATABASE", Impact.HIGH, Urgency.MEDIUM, 80, "aisha", "ananya", 12, 12, 70,
                "Index on orders.customer_id was dropped by a schema migration. Recreated the index; query time back to 40 ms.",
                "Missing index after migration", 5);
        h("Database server disk almost full", "Monitoring shows the database data volume at 92% and growing.",
                "Core Database", "DATABASE", Impact.MEDIUM, Urgency.HIGH, 20, "dev", "ananya", 10, 8, 55,
                "Archived old audit logs to cold storage and expanded the data volume by 200 GB.", "Log growth", 4);
        h("Replication lag on reporting database", "Sales reports show yesterday's numbers; the replica is hours behind.",
                "Core Database", "DATABASE", Impact.MEDIUM, Urgency.MEDIUM, 15, "vikram", "ananya", 5, 25, 150,
                "A long-running analytics query blocked replication. Moved the report to an off-peak schedule and added a statement timeout.",
                "Long-running query", 4);
        h("Cannot log in to any application - SSO error", "Login page says 'invalid signature' for every app that uses single sign-on.",
                "Single Sign-On", "AUTHENTICATION", Impact.HIGH, Urgency.HIGH, 400, "meera", "sneha", 11, 3, 28,
                "SSO signing certificate was rotated without updating relying parties. Re-published the federation metadata to all apps.",
                "Certificate rotation", 5);
        h("Account locked after password reset", "My account keeps getting locked a few minutes after I reset my password.",
                "Single Sign-On", "AUTHENTICATION", Impact.LOW, Urgency.HIGH, 1, "vikram", "sneha", 8, 30, 110,
                "Old password cached on the user's phone mail app caused repeated failed logins. Unlocked the account and removed the stale credentials.",
                "Cached credentials on mobile device", 5);
        h("MFA codes not accepted", "The authenticator app codes are rejected when I log in.",
                "Single Sign-On", "AUTHENTICATION", Impact.LOW, Urgency.MEDIUM, 1, "dev", "sneha", 4, 40, 200,
                "Time drift on the user's phone. Enabled automatic time sync and re-registered the authenticator.", "Device clock drift", 4);
        h("VPN disconnects every few minutes", "When working from home the VPN drops every 5-10 minutes and I lose my session.",
                "Corporate VPN", "NETWORK", Impact.MEDIUM, Urgency.MEDIUM, 30, "aisha", "arjun", 12, 30, 240,
                "Known keep-alive bug in VPN client 4.1. Upgraded all clients to 4.3 through the software centre.",
                "VPN client bug", 5);
        h("Wi-Fi not working on 3rd floor", "No Wi-Fi signal on the third floor east wing since this morning.",
                "Corporate Network", "NETWORK", Impact.MEDIUM, Urgency.HIGH, 40, "meera", "karan", 6, 10, 75,
                "Access point AP-3F-02 had failed. Replaced the hardware and rebalanced Wi-Fi channels.", "Hardware failure", 5);
        h("Office internet very slow", "Web pages take ages to load and video calls keep freezing.",
                "Corporate Network", "NETWORK", Impact.HIGH, Urgency.MEDIUM, 120, "vikram", "arjun", 3, 15, 100,
                "Internet link saturated by backup traffic. Applied a QoS policy limiting backups during business hours.",
                "Bandwidth saturation by backups", 4);
        h("Emails stuck in outbox", "Messages stay in my outbox and are never sent.",
                "Email & Calendar", "EMAIL", Impact.LOW, Urgency.MEDIUM, 1, "rohan", "sneha", 9, 50, 180,
                "Mailbox was over quota. Increased quota to 50 GB and enabled the online archive.", "Mailbox quota", 4);
        h("Not receiving external emails", "Nobody in sales has received emails from customers since yesterday evening.",
                "Email & Calendar", "EMAIL", Impact.MEDIUM, Urgency.HIGH, 25, "vikram", "sneha", 2, 8, 90,
                "A spam-filter rule was misconfigured after an update. Corrected the inbound connector rule and released quarantined mail.",
                "Spam filter misconfiguration", 5);
        h("HR portal shows error 500 when submitting leave", "Submitting a leave request fails with error 500.",
                "HR Portal", "APPLICATION", Impact.MEDIUM, Urgency.LOW, 12, "meera", "rahul", 10, 90, 600,
                "Null pointer in the leave approval workflow for employees without a manager. Fixed the data and deployed a code patch.",
                "Software defect", 4);
        h("Shared drive files not syncing", "Changes I make to files on the shared drive do not appear for my team.",
                "Cloud Storage", "STORAGE", Impact.MEDIUM, Urgency.MEDIUM, 8, "aisha", "ananya", 8, 35, 160,
                "Sync client token had expired. Signed out and back in and cleared the sync cache.", "Expired sync token", 4);
        h("Cloud storage upload failing for large files", "Uploading files larger than 100 MB fails at 99%.",
                "Cloud Storage", "STORAGE", Impact.LOW, Urgency.LOW, 3, "dev", "ananya", 6, 120, 700,
                "Proxy had a 100 MB request limit. Raised the limit to 2 GB for storage endpoints.", "Proxy upload limit", 5);
        h("Laptop not powering on", "My laptop does not turn on even when plugged in.",
                "End-User Devices", "HARDWARE", Impact.LOW, Urgency.HIGH, 1, "rohan", "karan", 11, 45, 220,
                "Faulty power adapter. Replaced the adapter; laptop charges normally.", "Hardware failure", 5);
        h("Laptop screen flickering", "Screen flickers constantly after the latest update.",
                "End-User Devices", "HARDWARE", Impact.LOW, Urgency.MEDIUM, 1, "meera", "karan", 7, 60, 300,
                "Display driver problem after update. Rolled back the GPU driver to the last stable version.", "Driver regression", 4);
        h("Printer not printing on 2nd floor", "Jobs sent to the 2nd floor printer stay in the queue.",
                "End-User Devices", "HARDWARE", Impact.MEDIUM, Urgency.LOW, 15, "dev", "karan", 5, 100, 400,
                "Print spooler had crashed. Restarted the spooler service and cleared the queue.", "Spooler crash", 3);
        h("CRM dashboard not loading", "The CRM home dashboard spins forever after this week's release.",
                "CRM", "APPLICATION", Impact.MEDIUM, Urgency.MEDIUM, 20, "vikram", "rahul", 4, 20, 140,
                "Stale browser cache after the CRM release. Cleared caches and fixed the CDN cache headers.", "Caching after release", 4);
        h("CRM sync with email failing", "Emails are no longer logged against customer records in CRM.",
                "CRM", "APPLICATION", Impact.MEDIUM, Urgency.LOW, 10, "vikram", "rahul", 1, 70, 330,
                "OAuth token for the CRM email integration had expired. Re-authorised the integration.", "Expired OAuth token", null);
        h("Suspicious phishing email received", "Got an email asking me to re-enter my password on a strange website.",
                "Email & Calendar", "SECURITY", Impact.MEDIUM, Urgency.HIGH, 50, "meera", "sneha", 3, 5, 45,
                "Phishing confirmed. Blocked the sender domain and purged the message from all mailboxes.", "Phishing campaign", 5);
        h("Antivirus alert on workstation", "Antivirus popped up saying a threat was found.",
                "End-User Devices", "SECURITY", Impact.LOW, Urgency.MEDIUM, 1, "dev", "arjun", 9, 20, 130,
                "Potentially unwanted program quarantined; full scan clean.", "Unwanted software", 5);
        h("Cloud VM unreachable", "Internal build server VM cannot be reached over HTTPS.",
                "Cloud Platform", "CLOUD", Impact.MEDIUM, Urgency.HIGH, 25, "aisha", "ananya", 2, 10, 60,
                "Security group rule for port 443 was removed by mistake. Restored the inbound rule.", "Configuration change", 5);
        h("Software installation request blocked", "I need Python installed but the installer asks for admin rights.",
                "End-User Devices", "SOFTWARE", Impact.LOW, Urgency.LOW, 1, "aisha", "karan", 6, 200, 900,
                "Installed the approved Python version through the software centre.", null, 5);
        h("Excel crashing when opening large file", "Excel closes as soon as I open the monthly finance workbook.",
                "End-User Devices", "SOFTWARE", Impact.LOW, Urgency.MEDIUM, 1, "rohan", "karan", 3, 90, 360,
                "Faulty add-in crashed Excel. Updated Office to the latest build and disabled the add-in.", "Faulty add-in", 4);
        // A late resolution: shows up in SLA violation reports.
        h("Payment reconciliation report missing", "The nightly payment reconciliation report was not generated.",
                "Payment Gateway", "APPLICATION", Impact.MEDIUM, Urgency.HIGH, 5, "rohan", "rahul", 8, 25, 190,
                "Scheduler job failed after a password change on the service account. Updated the stored credential and re-ran the job.",
                "Service account password change", 3);
    }

    /** Creates one historical incident and walks it through the full lifecycle with an audit trail. */
    private Incident h(String title, String description, String service, String category, Impact impact, Urgency urgency,
                       int affected, String reporter, String tech, int daysAgo, int ackMinutes, int resolveMinutes,
                       String resolution, String rootCause, Integer rating) {
        LocalDateTime created = now.minusDays(daysAgo).withHour(9 + (title.length() % 7)).withMinute((title.length() * 7) % 60);
        Incident i = createClassified(title, description, service, category, impact, urgency, affected, reporter, created);
        Technician t = (Technician) u.get(tech);
        step(i, created.plusMinutes(1), null, "ASSIGNED", () -> i.assignTo(t, created.plusMinutes(1)),
                "Assigned to " + t + " by Smart recommendation (weighted) strategy");
        step(i, created.plusMinutes(ackMinutes), t, "ACKNOWLEDGE", () -> i.acknowledge(created.plusMinutes(ackMinutes)),
                "Technician acknowledged the incident");
        step(i, created.plusMinutes(ackMinutes + 2), t, "START_WORK", () -> i.startWork(created.plusMinutes(ackMinutes + 2)),
                "Investigation started");
        LocalDateTime resolved = created.plusMinutes(resolveMinutes);
        step(i, resolved, t, "RESOLVE", () -> i.resolve(resolution, rootCause, resolved), "Resolution provided: " + resolution);
        if (rating != null) {
            LocalDateTime verified = resolved.plusMinutes(45);
            step(i, verified, i.getReporter(), "VERIFY", () -> i.verify(rating, rating >= 4 ? "Thanks, working now" : null, verified),
                    "Reporter confirmed the resolution (rating " + rating + "/5)");
            LocalDateTime closed = verified.plusMinutes(30);
            step(i, closed, null, "CLOSE", () -> i.close("Closed after verification", closed), "Incident closed");
        }
        recordBreaches(i);
        return i;
    }

    private Incident createClassified(String title, String description, String service, String category, Impact impact,
                                      Urgency urgency, int affected, String reporter, LocalDateTime created) {
        Incident i = incidents.save(new Incident(title, description, u.get(reporter), svc.get(service),
                impact, urgency, affected, created));
        log(i, created, i.getReporter(), "CREATED", null, IncidentStatus.NEW,
                "Incident created by " + i.getReporter().getName() + " for service " + service);
        step(i, created, null, "CLASSIFIED", () -> i.classify(cat.get(category), ClassificationSource.KEYWORD, 0.82, created),
                "Classified as " + cat.get(category).getName() + " by Keyword/TF-IDF classifier (82% confidence)");
        PriorityResult p = priorityService.calculate(i);
        SlaPolicy policy = slaService.policyFor(p.priority(), i.getService());
        i.applyPriority(p, policy, created);
        log(i, created, null, "PRIORITY_SET", i.getStatus(), i.getStatus(),
                "Priority calculated as " + p.priority() + " [" + p.rationale() + "]");
        return i;
    }

    private void step(Incident i, LocalDateTime at, User actor, String action, Runnable change, String description) {
        IncidentStatus from = i.getStatus();
        change.run();
        log(i, at, actor, action, from, i.getStatus(), description);
    }

    private void log(Incident i, LocalDateTime at, User actor, String action, IncidentStatus from, IncidentStatus to, String description) {
        i.addAuditEntry(audit.save(new AuditEntry(i, actor, at, action, from, to, description)));
    }

    private void recordBreaches(Incident i) {
        if (i.getSla().isResponseBreached()) {
            violations.save(new SlaViolation(i, SlaBreachType.RESPONSE, i.getSla().getResponseDeadline().plusMinutes(1)));
        }
        if (i.getSla().isResolutionBreached()) {
            violations.save(new SlaViolation(i, SlaBreachType.RESOLUTION, i.getSla().getResolutionDeadline().plusMinutes(1)));
        }
    }

    // ------------------------------------------------------------------ live incidents

    private void seedLiveIncidents() {
        // In progress for a while: VPN problem for remote staff.
        LocalDateTime t1 = now.minusMinutes(70);
        Incident vpn = createClassified("VPN connection drops for remote staff",
                "Several people working from home say the VPN disconnects every few minutes since this morning.",
                "Corporate VPN", "NETWORK", Impact.MEDIUM, Urgency.HIGH, 25, "aisha", t1);
        Technician arjun = (Technician) u.get("arjun");
        step(vpn, t1.plusMinutes(1), null, "ASSIGNED", () -> vpn.assignTo(arjun, t1.plusMinutes(1)), "Assigned to " + arjun);
        step(vpn, t1.plusMinutes(9), arjun, "ACKNOWLEDGE", () -> vpn.acknowledge(t1.plusMinutes(9)), "Technician acknowledged the incident");
        step(vpn, t1.plusMinutes(12), arjun, "START_WORK", () -> vpn.startWork(t1.plusMinutes(12)), "Investigation started");
        vpn.addComment(arjun, "Checked VPN concentrator logs: keep-alive timeouts for clients on version 4.1 again. Testing 4.3 rollout.",
                CommentType.INVESTIGATION_NOTE, t1.plusMinutes(40));

        // Waiting for the reporter: SLA clock paused.
        LocalDateTime t2 = now.minusHours(5);
        Incident keyboard = createClassified("Laptop keyboard keys not working",
                "Some keys on my laptop keyboard (E, R and T) stopped working.",
                "End-User Devices", "HARDWARE", Impact.LOW, Urgency.LOW, 1, "dev", t2);
        Technician karan = (Technician) u.get("karan");
        step(keyboard, t2.plusMinutes(2), null, "ASSIGNED", () -> keyboard.assignTo(karan, t2.plusMinutes(2)), "Assigned to " + karan);
        step(keyboard, t2.plusMinutes(30), karan, "ACKNOWLEDGE", () -> keyboard.acknowledge(t2.plusMinutes(30)), "Technician acknowledged the incident");
        step(keyboard, t2.plusMinutes(35), karan, "START_WORK", () -> keyboard.startWork(t2.plusMinutes(35)), "Investigation started");
        step(keyboard, t2.plusMinutes(50), karan, "REQUEST_INFO", () -> keyboard.requestInfo(t2.plusMinutes(50)),
                "Technician requested more information (SLA paused)");
        keyboard.addComment(karan, "Could you tell me your laptop's asset tag (sticker underneath) and whether an external keyboard works?",
                CommentType.INFO_REQUEST, t2.plusMinutes(50));

        // Resolved, waiting for the employee to verify: demo of the feedback step.
        LocalDateTime t3 = now.minusHours(6);
        Incident leave = createClassified("HR portal leave balance incorrect",
                "My leave balance shows 2 days but I should have 12 days left this year.",
                "HR Portal", "APPLICATION", Impact.LOW, Urgency.LOW, 1, "meera", t3);
        Technician rahul = (Technician) u.get("rahul");
        step(leave, t3.plusMinutes(3), null, "ASSIGNED", () -> leave.assignTo(rahul, t3.plusMinutes(3)), "Assigned to " + rahul);
        step(leave, t3.plusMinutes(40), rahul, "ACKNOWLEDGE", () -> leave.acknowledge(t3.plusMinutes(40)), "Technician acknowledged the incident");
        step(leave, t3.plusMinutes(45), rahul, "START_WORK", () -> leave.startWork(t3.plusMinutes(45)), "Investigation started");
        step(leave, t3.plusMinutes(150), rahul, "RESOLVE",
                () -> leave.resolve("Carry-forward days from last year were not imported. Re-ran the import; balance now shows 12 days.",
                        "Missed data import", t3.plusMinutes(150)),
                "Resolution provided");

        // Classified but nobody assigned: demo of smart recommendations for the admin.
        LocalDateTime t4 = now.minusMinutes(25);
        createClassified("Shared drive access denied for finance team",
                "Finance team gets 'access denied' when opening the Q3 folder on the shared drive.",
                "Cloud Storage", "STORAGE", Impact.MEDIUM, Urgency.MEDIUM, 6, "rohan", t4);
    }
}
