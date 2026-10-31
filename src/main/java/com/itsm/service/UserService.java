package com.itsm.service;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.user.*;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.ValidationException;
import com.itsm.repository.CategoryRepository;
import com.itsm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Registration and user administration. */
@Service
@Transactional
public class UserService {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository users;
    private final CategoryRepository categories;
    private final UserFactory factory;

    public UserService(UserRepository users, CategoryRepository categories, UserFactory factory) {
        this.users = users;
        this.categories = categories;
        this.factory = factory;
    }

    /** Self-registration is for employees only; staff accounts are created by an administrator. */
    public User registerEmployee(String name, String email, String password, String department) {
        return create(Role.EMPLOYEE, name, email, password, department, null);
    }

    public User create(Role role, String name, String email, String password, String detail, String phone) {
        validateName(name);
        validateEmail(email);
        validatePassword(password);
        if (users.existsByEmailIgnoreCase(email.trim())) {
            throw new ValidationException("An account with this email already exists");
        }
        User user = factory.create(role, name.trim(), email, password, detail);
        user.updateProfile(name.trim(), blankToNull(phone));
        return users.save(user);
    }

    public void updateUser(Long id, String name, String email, String phone, String detail, boolean active, User actor) {
        User user = get(id);
        validateName(name);
        validateEmail(email);
        if (!user.getEmail().equalsIgnoreCase(email.trim()) && users.existsByEmailIgnoreCase(email.trim())) {
            throw new ValidationException("An account with this email already exists");
        }
        if (user.equals(actor) && !active) {
            throw new ValidationException("You cannot deactivate your own account");
        }
        user.updateProfile(name.trim(), blankToNull(phone));
        user.changeEmail(email);
        if (user instanceof Employee e) {
            e.setDepartment(blankToNull(detail));
        } else if (user instanceof Administrator a) {
            a.setJobTitle(blankToNull(detail));
        }
        if (active) user.activate(); else user.deactivate();
    }

    public void updateTechnician(Long id, Set<Long> skillIds, int maxActive, int experienceYears,
                                 boolean available, boolean teamLead) {
        Technician t = getTechnician(id);
        Set<Category> skills = new HashSet<>(categories.findAllById(skillIds == null ? Set.of() : skillIds));
        t.replaceSkills(skills);
        try {
            t.setMaxActiveIncidents(maxActive);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
        t.setExperienceYears(experienceYears);
        t.setAvailable(available);
        t.setTeamLead(teamLead);
    }

    /** Technicians can toggle their own availability (e.g. going off shift). */
    public void setAvailability(Technician technician, boolean available) {
        getTechnician(technician.getId()).setAvailable(available);
    }

    public void resetPassword(Long id, String newPassword) {
        validatePassword(newPassword);
        get(id).changePassword(factory.hash(newPassword));
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public Technician getTechnician(Long id) {
        return users.findById(id).filter(Technician.class::isInstance).map(Technician.class::cast)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", id));
    }

    @Transactional(readOnly = true)
    public List<User> all() {
        return users.findAll().stream()
                .sorted((a, b) -> a.getRole() == b.getRole() ? a.getName().compareToIgnoreCase(b.getName())
                        : a.getRole().compareTo(b.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Technician> technicians() {
        return users.findAllTechnicians();
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            throw new ValidationException("Name is required (max 100 characters)");
        }
    }

    private static void validateEmail(String email) {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new ValidationException("A valid email address is required");
        }
    }

    static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || !password.chars().anyMatch(Character::isDigit) || !password.chars().anyMatch(Character::isLetter)) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters and contain letters and digits");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
