package com.itsm.security;

import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The logged-in user as Spring Security sees it. Holds only the id, email, name, role
 * and password hash (no JPA entity in the session), so the session stays small and serialisable.
 */
public class ItsmUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String name;
    private final Role role;
    private final String passwordHash;
    private final boolean active;

    public ItsmUserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.role = user.getRole();
        this.passwordHash = user.getPasswordHash();
        this.active = user.isActive();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Role getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
