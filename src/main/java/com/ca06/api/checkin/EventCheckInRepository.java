package com.ca06.api.checkin;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventCheckInRepository extends JpaRepository<EventCheckIn, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    Optional<EventCheckIn> findByUserIdAndEventId(Long userId, Long eventId);

    List<EventCheckIn> findAllByEventIdOrderByCheckedInAtAsc(Long eventId);
}
