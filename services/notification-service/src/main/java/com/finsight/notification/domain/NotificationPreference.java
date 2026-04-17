package com.finsight.notification.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences")
@IdClass(NotificationPreferenceId.class)
public class NotificationPreference {

    @Id
    private String ownerEmail;

    @Id
    private String type;

    private boolean enabled = true;

    protected NotificationPreference() {}

    public NotificationPreference(String ownerEmail, String type, boolean enabled) {
        this.ownerEmail = ownerEmail;
        this.type = type;
        this.enabled = enabled;
    }

    public String getOwnerEmail() { return ownerEmail; }
    public String getType() { return type; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
