package com.itsm.service;

import com.itsm.domain.catalog.BusinessCriticality;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.sla.SlaPolicy;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.ValidationException;
import com.itsm.repository.CategoryRepository;
import com.itsm.repository.ItServiceRepository;
import com.itsm.repository.SlaPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Administration of categories, IT services (with dependencies) and SLA policies. */
@Service
@Transactional
public class CatalogService {

    private final CategoryRepository categories;
    private final ItServiceRepository services;
    private final SlaPolicyRepository slaPolicies;

    public CatalogService(CategoryRepository categories, ItServiceRepository services, SlaPolicyRepository slaPolicies) {
        this.categories = categories;
        this.services = services;
        this.slaPolicies = slaPolicies;
    }

    // ---------------------------------------------------------------- categories

    @Transactional(readOnly = true)
    public List<Category> allCategories() {
        return categories.findAllByOrderByName();
    }

    @Transactional(readOnly = true)
    public List<Category> activeCategories() {
        return categories.findByActiveTrueOrderByName();
    }

    public Category createCategory(String code, String name, String description, String keywords) {
        if (code == null || !code.trim().matches("[A-Za-z_]{2,40}")) {
            throw new ValidationException("Code must be 2–40 letters or underscores");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("Category name is required");
        }
        if (categories.findByCodeIgnoreCase(code.trim()).isPresent()) {
            throw new ValidationException("Category code " + code.toUpperCase() + " already exists");
        }
        return categories.save(new Category(code, name.trim(), description, keywords));
    }

    public void updateCategory(Long id, String name, String description, String keywords, boolean active) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Category name is required");
        }
        categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id))
                .update(name.trim(), description, keywords, active);
    }

    // ---------------------------------------------------------------- services

    @Transactional(readOnly = true)
    public List<ItService> allServices() {
        return services.findAllByOrderByName();
    }

    @Transactional(readOnly = true)
    public List<ItService> activeServices() {
        return services.findByActiveTrueOrderByName();
    }

    @Transactional(readOnly = true)
    public ItService getService(Long id) {
        return services.findById(id).orElseThrow(() -> new ResourceNotFoundException("IT service", id));
    }

    public ItService createService(String name, String description, BusinessCriticality criticality,
                                   Long defaultCategoryId, Set<Long> dependsOnIds) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Service name is required");
        }
        if (services.findByNameIgnoreCase(name.trim()).isPresent()) {
            throw new ValidationException("A service named " + name + " already exists");
        }
        ItService service = services.save(new ItService(name.trim(), description, criticality, category(defaultCategoryId)));
        service.replaceDependencies(dependencies(service, dependsOnIds));
        return service;
    }

    public void updateService(Long id, String name, String description, BusinessCriticality criticality,
                              Long defaultCategoryId, Set<Long> dependsOnIds, boolean active) {
        ItService service = getService(id);
        services.findByNameIgnoreCase(name.trim()).filter(other -> !other.equals(service)).ifPresent(other -> {
            throw new ValidationException("A service named " + name + " already exists");
        });
        service.update(name.trim(), description, criticality, category(defaultCategoryId), active);
        service.replaceDependencies(dependencies(service, dependsOnIds));
    }

    private Set<ItService> dependencies(ItService service, Set<Long> ids) {
        Set<ItService> deps = new LinkedHashSet<>(services.findAllById(ids == null ? Set.of() : ids));
        deps.remove(service);
        for (ItService dep : deps) {
            if (reaches(dep, service, new HashSet<>())) {
                throw new ValidationException("Circular dependency: " + dep.getName() + " already depends on " + service.getName());
            }
        }
        return deps;
    }

    /** Depth-first search over the dependency graph to prevent cycles. */
    private boolean reaches(ItService from, ItService target, Set<ItService> visited) {
        if (from.equals(target)) return true;
        if (!visited.add(from)) return false;
        for (ItService next : from.getDependsOn()) {
            if (reaches(next, target, visited)) return true;
        }
        return false;
    }

    private Category category(Long id) {
        return id == null ? null : categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    // ---------------------------------------------------------------- SLA policies

    @Transactional(readOnly = true)
    public List<SlaPolicy> defaultPolicies() {
        return slaPolicies.findByServiceIsNullOrderByPriority();
    }

    @Transactional(readOnly = true)
    public List<SlaPolicy> serviceOverrides() {
        return slaPolicies.findByServiceIsNotNullOrderByPriority();
    }

    public void saveSlaPolicy(Priority priority, Long serviceId, int responseMinutes, int resolutionMinutes) {
        ItService service = serviceId == null ? null : getService(serviceId);
        Optional<SlaPolicy> existing = service == null
                ? slaPolicies.findByPriorityAndServiceIsNull(priority)
                : slaPolicies.findByPriorityAndService(priority, service);
        try {
            if (existing.isPresent()) {
                existing.get().setTargets(responseMinutes, resolutionMinutes);
            } else {
                slaPolicies.save(new SlaPolicy(priority, service, responseMinutes, resolutionMinutes));
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    public void deleteSlaOverride(Long policyId) {
        SlaPolicy policy = slaPolicies.findById(policyId).orElseThrow(() -> new ResourceNotFoundException("SLA policy", policyId));
        if (policy.isDefault()) {
            throw new ValidationException("Default policies cannot be deleted; edit them instead");
        }
        slaPolicies.delete(policy);
    }
}
