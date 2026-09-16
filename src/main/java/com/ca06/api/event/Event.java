package com.ca06.api.event;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(name = "check_in_token", nullable = false, unique = true, length = 36)
    private String checkInToken;

    @Column(name = "check_in_starts_at")
    private LocalDateTime checkInStartsAt;

    @Column(name = "check_in_ends_at")
    private LocalDateTime checkInEndsAt;

    protected Event() {
    }

    public Event(
            String name,
            String description,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            String location,
            LocalDateTime checkInStartsAt,
            LocalDateTime checkInEndsAt) {
        this.name = name;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.location = location;
        this.checkInStartsAt = checkInStartsAt;
        this.checkInEndsAt = checkInEndsAt;
    }

    @PrePersist
    void ensureCheckInToken() {
        if (checkInToken == null || checkInToken.isBlank()) {
            checkInToken = UUID.randomUUID().toString();
        }
    }

    public EventStatus status(LocalDateTime now) {
        if (now.isAfter(endsAt)) {
            return EventStatus.FINISHED;
        }
        if (now.isBefore(startsAt)) {
            return EventStatus.UPCOMING;
        }
        return EventStatus.AVAILABLE;
    }

    public boolean isCheckInAvailable(LocalDateTime now) {
        if (status(now) == EventStatus.FINISHED) {
            return false;
        }
        if (checkInStartsAt != null && now.isBefore(checkInStartsAt)) {
            return false;
        }
        if (checkInEndsAt != null && now.isAfter(checkInEndsAt)) {
            return false;
        }
        return true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public String getLocation() {
        return location;
    }

    public String getCheckInToken() {
        return checkInToken;
    }

    public LocalDateTime getCheckInStartsAt() {
        return checkInStartsAt;
    }

    public LocalDateTime getCheckInEndsAt() {
        return checkInEndsAt;
    }
}
