package com.itsm.domain.catalog;

import jakarta.persistence.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * An incident category such as NETWORK or DATABASE.
 * <p>
 * Categories are data, not an enum, so an administrator can add a new one at runtime
 * without any code change. The keywords drive automatic categorisation.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 500)
    private String description;

    /** Comma-separated keywords used by the keyword classifier. */
    @Column(length = 1000)
    private String keywords;

    @Column(nullable = false)
    private boolean active = true;

    protected Category() {
        // for JPA
    }

    public Category(String code, String name, String description, String keywords) {
        this.code = code.trim().toUpperCase();
        this.name = name;
        this.description = description;
        this.keywords = keywords;
    }

    public void update(String name, String description, String keywords, boolean active) {
        this.name = name;
        this.description = description;
        this.keywords = keywords;
        this.active = active;
    }

    public List<String> keywordList() {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(k -> !k.isEmpty())
                .toList();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getKeywords() { return keywords; }
    public boolean isActive() { return active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category other)) return false;
        return getCode() != null && getCode().equals(other.getCode());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getCode());
    }

    @Override
    public String toString() {
        return name;
    }
}
