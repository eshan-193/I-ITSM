package com.itsm.repository;

import com.itsm.domain.user.Administrator;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Repository pattern: services ask for users by meaning, never by SQL. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select t from Technician t order by t.name")
    List<Technician> findAllTechnicians();

    @Query("select t from Technician t where t.active = true order by t.name")
    List<Technician> findActiveTechnicians();

    @Query("select t from Technician t where t.active = true and t.teamLead = true")
    List<Technician> findTeamLeads();

    @Query("select a from Administrator a where a.active = true")
    List<Administrator> findActiveAdministrators();

    @Query("select u from User u where type(u) = :type order by u.name")
    List<User> findByType(Class<? extends User> type);

    default List<User> findByRole(Role role) {
        return switch (role) {
            case EMPLOYEE -> findByType(com.itsm.domain.user.Employee.class);
            case TECHNICIAN -> findByType(Technician.class);
            case ADMIN -> findByType(Administrator.class);
        };
    }
}
