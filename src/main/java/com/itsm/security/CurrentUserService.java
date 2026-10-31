package com.itsm.security;

import com.itsm.domain.user.User;
import com.itsm.exception.UnauthorizedOperationException;
import com.itsm.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Gives controllers the logged-in user as a full domain object. */
@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public Optional<ItsmUserPrincipal> principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ItsmUserPrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public User require() {
        Long id = principal().map(ItsmUserPrincipal::getId)
                .orElseThrow(() -> new UnauthorizedOperationException("Please log in"));
        return users.findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedOperationException("Your account is no longer active"));
    }
}
