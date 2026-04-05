package com.finsight.notification.persistence;

import com.finsight.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.ownerEmail = :ownerEmail")
    void markAllAsReadByOwnerEmail(@Param("ownerEmail") String ownerEmail);
}
