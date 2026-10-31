package com.itsm.security;

import com.itsm.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads users by email for Spring Security's login check. */
@Service
public class ItsmUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public ItsmUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmailIgnoreCase(email.trim())
                .map(ItsmUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }
}
