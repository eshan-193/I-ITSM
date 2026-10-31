package com.itsm.repository;

import com.itsm.domain.notification.Notification;
import com.itsm.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByRecipientOrderByCreatedAtDesc(User recipient);

    List<Notification> findTop8ByRecipientOrderByCreatedAtDesc(User recipient);

    long countByRecipientAndReadFalse(User recipient);

    @Modifying
    @Query("update Notification n set n.read = true where n.recipient = :recipient and n.read = false")
    int markAllRead(User recipient);
}
