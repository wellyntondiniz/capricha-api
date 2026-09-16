CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(180) NOT NULL,
    description VARCHAR(1000),
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    location VARCHAR(255) NOT NULL,
    check_in_token CHAR(36) NOT NULL,
    check_in_starts_at DATETIME(6),
    check_in_ends_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT uk_events_check_in_token UNIQUE (check_in_token)
);

CREATE TABLE event_checkin (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    checked_in_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_event_checkin PRIMARY KEY (id),
    CONSTRAINT uk_event_checkin_user_event UNIQUE (user_id, event_id),
    CONSTRAINT fk_event_checkin_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_event_checkin_event FOREIGN KEY (event_id) REFERENCES events (id),
    INDEX idx_event_checkin_event (event_id),
    INDEX idx_event_checkin_user (user_id)
);
