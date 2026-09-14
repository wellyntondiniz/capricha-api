package com.ca06.api.checkin;

import java.time.LocalDateTime;
import com.ca06.api.event.Event;
import com.ca06.api.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "event_checkin",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_checkin_user_event",
                columnNames = {"user_id", "event_id"}))
public class EventCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    protected EventCheckIn() {
    }

    public EventCheckIn(AppUser user, Event event, LocalDateTime checkedInAt) {
        this.user = user;
        this.event = event;
        this.checkedInAt = checkedInAt;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public Event getEvent() {
        return event;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }
}
