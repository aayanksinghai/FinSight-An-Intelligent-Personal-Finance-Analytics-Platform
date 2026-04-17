package com.finsight.notification.domain;

import java.io.Serializable;
import java.util.Objects;

public class NotificationPreferenceId implements Serializable {
    private String ownerEmail;
    private String type;

    public NotificationPreferenceId() {}
    public NotificationPreferenceId(String ownerEmail, String type) {
        this.ownerEmail = ownerEmail;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NotificationPreferenceId that)) return false;
        return Objects.equals(ownerEmail, that.ownerEmail) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() { return Objects.hash(ownerEmail, type); }
}
