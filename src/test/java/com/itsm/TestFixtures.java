package com.itsm;

import com.itsm.domain.catalog.BusinessCriticality;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.user.Administrator;
import com.itsm.domain.user.Employee;
import com.itsm.domain.user.Technician;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

/** Builders for plain domain objects used by unit tests (no database, no Spring). */
public final class TestFixtures {

    public static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 1, 9, 0);

    private static long nextId = 100;

    private TestFixtures() {
    }

    public static Category category(String code) {
        return withId(new Category(code, code.charAt(0) + code.substring(1).toLowerCase(), null, ""));
    }

    public static ItService service(String name, BusinessCriticality criticality) {
        return withId(new ItService(name, null, criticality, null));
    }

    public static Employee employee(String name) {
        return withId(new Employee(name, name.toLowerCase() + "@test.local", "hash", "Dept"));
    }

    public static Technician technician(String name, int maxActive, int years, Category... skills) {
        Technician t = withId(new Technician(name, name.toLowerCase() + "@test.local", "hash", maxActive, years));
        for (Category c : skills) {
            t.addSkill(c);
        }
        return t;
    }

    public static Administrator admin() {
        return withId(new Administrator("Admin", "admin@test.local", "hash", "IT Manager"));
    }

    public static Incident incident(String title, Employee reporter, ItService service, Impact impact, Urgency urgency) {
        return withId(new Incident(title, title + " – details of the problem", reporter, service, impact, urgency, 1, T0));
    }

    /** Sets the private JPA id, which is normally assigned by the database. */
    public static <T> T withId(T entity) {
        try {
            Class<?> c = entity.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(entity, nextId++);
                    return entity;
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            throw new IllegalStateException("No id field on " + entity.getClass());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
