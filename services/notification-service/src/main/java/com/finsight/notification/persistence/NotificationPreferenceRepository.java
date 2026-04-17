package com.finsight.notification.persistence;

import com.finsight.notification.domain.NotificationPreference;
import com.finsight.notification.domain.NotificationPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationPreferenceRepository
        extends JpaRepository<NotificationPreference, NotificationPreferenceId> {

    List<NotificationPreference> findByOwnerEmail(String ownerEmail);
}
